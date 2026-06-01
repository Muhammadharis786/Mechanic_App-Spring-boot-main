package com.haris.MechanicApp.Service;

import com.haris.MechanicApp.Controller.LiveLocationController;
import com.haris.MechanicApp.Model.GoogleDistance;
import com.haris.MechanicApp.Model.Location.LocationDTO;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Mechanic.NearbyMechanicDTO;
import com.haris.MechanicApp.Model.Mechanic.NearbyMechanicMapResponseDto;
import com.haris.MechanicApp.Model.RequestService.*;
import com.haris.MechanicApp.Model.RoadInfo;
import com.haris.MechanicApp.Model.Verification.User;
import com.haris.MechanicApp.Repository.MechanicRepository;
import com.haris.MechanicApp.Repository.ServiceRequestRepository;
import com.haris.MechanicApp.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class ServiceRequestService {

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MechanicRepository mechanicRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    public ResponseEntity<?> createRequest(CreateServiceRequestDto dto, String userPhoneNumber) {

        Optional<User> userOptional = userRepository.findByPhonenumber(userPhoneNumber);

        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User Not Found");
        }

        User user = userOptional.get();

        RequestService request = new RequestService();
        request.setUser(user);
        request.setUserLatitude(dto.getUserLatitude());
        request.setUserLongitude(dto.getUserLongitude());
        request.setLocationName(dto.getLocationName());
        request.setServiceType(dto.getServiceType());
        request.setUserNotes(dto.getUserNotes());
        request.setIsFixedChargeAccepted(dto.isIsfixedchargeaccepted());
        request.setRequestStatus(ServiceRequestStatus.PENDING);
        request.setPaymentStatus("UNPAID");

        RequestService savedRequest = serviceRequestRepository.save(request);

       return sendRequestToNearbyOnlineMechanics(savedRequest);

//        return ResponseEntity.status(HttpStatus.CREATED).body(savedRequest);
    }

    private ResponseEntity<?> sendRequestToNearbyOnlineMechanics(RequestService request) {

        GeoOperations<String, String> geoOperations = redisTemplate.opsForGeo();

        GeoResults<RedisGeoCommands.GeoLocation<String>> nearbyMechanics =
                geoOperations.search(
                        "mechanic",
                        GeoReference.fromCoordinate(request.getUserLongitude(), request.getUserLatitude()),
                        new Distance(65, Metrics.KILOMETERS),
                        RedisGeoCommands.GeoSearchCommandArgs
                                .newGeoSearchArgs()
                                .includeDistance()
                                .includeCoordinates()

                );

        if (nearbyMechanics == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nearby Mechanics Not Available");
        }

        Map<Long, Point> mechanicPoints = new LinkedHashMap<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : nearbyMechanics) {
            Long mechanicId = Long.valueOf(result.getContent().getName());
            System.out.println("Abhi filhal mujhay nearby mechanics miay id hay:"+ mechanicId);
            boolean isOnline = Boolean.parseBoolean(
                    String.valueOf(
                            redisTemplate.opsForHash()
                                    .get("mechanic:details:" + mechanicId, "isOnline")
                    )
            );
                //may chek krnga kay wo mechanci online hay kay nh
            if (isOnline) {
                 // ager online hay tu may ye dekgnga ab kya wo online mechanci user kay servie type say
                //match krta hay kay nh
                System.out.println("Abhi filhal mujhay nearby mechanics aur online miay id hay:"+ mechanicId);
                String mechanictype =   (String) redisTemplate.opsForHash()
                        .get("mechanic:details:" + mechanicId, "serviceType");
                if (request.getServiceType().equalsIgnoreCase(mechanictype)){
                    // ager mechanic online hay aur servicetype bhi match hay tu may uski id map per add krdnga
                    System.out.println(" mujhay nearby mechanics aur online aur service type miay id hay:"+ mechanicId);
                    mechanicPoints.put(mechanicId, result.getContent().getPoint());
                }

            }
        }

        //ager mechanic online aur user ki service type say nh hay tu mechanicpoints empty hngay
        if (mechanicPoints.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic Not Online in your area");
        }

        List<Long> validMechanicIds = mechanicRepository.findAvailableMechanicIds(

                // key set mean jo mechanic online aur user k service type say match
                //wlay mechanic ki ids hay ye pass horha hay [55,57,59] ye wo mechanic hay jokay online and
                //service type match
                new ArrayList<>(mechanicPoints.keySet())

        );
        //        mechanic online bhi hona chie service type match hona chie engaged nh hona chie aur mechanci verified bhi hona chie
        //ager nh hay ye sari chizay tu empty hay
        if (validMechanicIds.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic not available");

        }

        String redisKey = "request:mechanics:" + request.getRequestId();

        for (Long mechanicId : validMechanicIds) {
            redisTemplate.opsForSet().add(
                    redisKey,
                    mechanicId.toString()
            );
            System.out.println("acha ye"+  mechanicId + " id redis may jarhi hay is key per request:mechanics:" + request.getRequestId());
        }


        StringBuilder destinationsparam = new StringBuilder();
        List<Long> orderedMechanicIds = new ArrayList<>();

        //may yha loop chala rha hn jokay mechanicpoints means ye wo mechanic hay
        //jokay online aur servicetype match hay ye usper loop nh jo not engage aur isverifeid hay
        for (Long mechanicId : mechanicPoints.keySet()){

            // ab yha mechancId jo mechanicpoint say ek ek krk kay nikaliga
            //usko may jo not engage aur isverifed ,online,servicetype mathc jo mechanic id list hay
                //check krha hay
            if (validMechanicIds.contains(mechanicId)) {
                //ager id match hay tu jis id kay wo points tha jo humnay upper loop may save kie thay
                Point point = mechanicPoints.get(mechanicId);
                System.out.println(" mujhay nearby mechanics aur online,servietype,notengage ,verified miay id hay:"+ mechanicId);

                destinationsparam
                        .append(point.getY())
                        .append(",")
                        .append(point.getX())
                        .append("|");

                orderedMechanicIds.add(mechanicId);
            }

        }


        if (!destinationsparam.isEmpty()) {
            destinationsparam.setLength(destinationsparam.length() - 1);
            System.out.println("Destination Param: " + destinationsparam.toString());
        }

        GoogleDistance googleapi = new GoogleDistance();

        List<RoadInfo> roadDistances = googleapi.getBatchRoadDistances(
                request.getUserLatitude(),
                request.getUserLongitude(),
                destinationsparam.toString()
        );
            List< MechanicRequestNotificationDto> dto =  new ArrayList<>();
        for (int i = 0; i < orderedMechanicIds.size(); i++) {
            Long mechanicId = orderedMechanicIds.get(i);

            RoadInfo roadInfo = null;

            if (i < roadDistances.size()) {
                roadInfo = roadDistances.get(i);
            }

            Double distanceKm = null;
            String eta = null;

            if (roadInfo != null && roadInfo.getDistance() >= 0) {
                distanceKm = roadInfo.getDistance();
                eta = roadInfo.getDistancetime();
            }

            MechanicRequestNotificationDto notificationDto =
                    new MechanicRequestNotificationDto(
                            request.getRequestId(),
                            request.getUser().getUserid(),
                            request.getServiceType(),
                            request.getUserNotes(),
                            request.getUserLatitude(),
                            request.getUserLongitude(),
                            request.getLocationName(),
                            distanceKm,
                            eta,
                            request.getRequestStatus(),
                            request.getUser().getUsername() ,
                            request.getUser().getUserimgurl()
                    );

            simpMessagingTemplate.convertAndSend(
                    "/topic/mechanic/requests/" + mechanicId,
                    notificationDto
            );
            System.out.println("is id wly kay pass jaiga request :"+ mechanicId);
            dto.add(notificationDto);

        }
        return  ResponseEntity.status(HttpStatus.OK).body(dto);
}

    public ResponseEntity<?> nearbyOnlineMechanics(String phonenumber  , LocationDTO location) {

        GeoOperations<String, String> geoOperations = redisTemplate.opsForGeo();

        Optional<User> checkuser = userRepository.findByPhonenumber(phonenumber);
        if (checkuser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        User user = checkuser.get();
        GeoResults<RedisGeoCommands.GeoLocation<String>> nearbyMechanics =
                geoOperations.search(
                        "mechanic",
                        GeoReference.fromCoordinate(location.getLongitude().doubleValue(), location.getLatitude().doubleValue()),
                        new Distance(65, Metrics.KILOMETERS),
                        RedisGeoCommands.GeoSearchCommandArgs
                                .newGeoSearchArgs()
                                .includeDistance()
                                .includeCoordinates()

                );

        if (nearbyMechanics == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nearby Mechanics Not Available");
        }

        Map<Long, Point> mechanicPoints = new LinkedHashMap<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : nearbyMechanics) {
            Long mechanicId = Long.valueOf(result.getContent().getName());
            System.out.println("Abhi filhal mujhay nearby mechanics miay id hay:" + mechanicId);
            boolean isOnline = Boolean.parseBoolean(
                    String.valueOf(
                            redisTemplate.opsForHash()
                                    .get("mechanic:details:" + mechanicId, "isOnline")
                    )
            );
            //may chek krnga kay wo mechanci online hay kay nh
            if (isOnline) {
                // ager online hay tu may ye dekgnga ab kya wo online mechanci user kay servie type say
                //match krta hay kay nh
                System.out.println("Abhi filhal mujhay nearby mechanics aur online miay id hay:" + mechanicId);
                String mechanictype = (String) redisTemplate.opsForHash()
                        .get("mechanic:details:" + mechanicId, "serviceType");
                if (location.getServiceType().equalsIgnoreCase(mechanictype)) {
                    // ager mechanic online hay aur servicetype bhi match hay tu may uski id map per add krdnga
                    System.out.println(" mujhay nearby mechanics aur online aur service type miay id hay:" + mechanicId);
                    mechanicPoints.put(mechanicId, result.getContent().getPoint());
                }

            }
        }

        //ager mechanic online aur user ki service type say nh hay tu mechanicpoints empty hngay
        if (mechanicPoints.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic Not Online in your area");
        }

        List<Long> validMechanicIds = mechanicRepository.findAvailableMechanicIds(

                // key set mean jo mechanic online aur user k service type say match
                //wlay mechanic ki ids hay ye pass horha hay [55,57,59] ye wo mechanic hay jokay online and
                //service type match
                new ArrayList<>(mechanicPoints.keySet())

        );
        //        mechanic online bhi hona chie service type match hona chie engaged nh hona chie aur mechanci verified bhi hona chie
        //ager nh hay ye sari chizay tu empty hay
        if (validMechanicIds.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic not available");

        }



        //may yha loop chala rha hn jokay mechanicpoints means ye wo mechanic hay
        //jokay online aur servicetype match hay ye usper loop nh jo not engage aur isverifeid hay
        List<NearbyMechanicDTO>     response = new ArrayList<>();
        for (Long mechanicId : mechanicPoints.keySet()) {

            // ab yha mechancId jo mechanicpoint say ek ek krk kay nikaliga
            //usko may jo not engage aur isverifed ,online,servicetype mathc jo mechanic id list hay
            //check krha hay
            if (validMechanicIds.contains(mechanicId)) {
                Point point = mechanicPoints.get(mechanicId);
                //ager id match hay tu jis id kay wo points tha jo humnay upper loop may save kie thay
                response.add(
                        new NearbyMechanicDTO(
                                mechanicId,
                                point.getY(), // latitude
                                point.getX()  // longitude
                        )
                );
                System.out.println(" mujhay nearby mechanics aur online,servietype,notengage ,verified miay id hay:" + mechanicId);


            }


        }



        String mapSessionId = "user-map-" + user.getUserid();
        saveMapSessionMechanics(mapSessionId, response);

        NearbyMechanicMapResponseDto mapResponse =
                new NearbyMechanicMapResponseDto(
                        mapSessionId,
                        response
                );

        return ResponseEntity.status(HttpStatus.OK).body(mapResponse);

    }

    private void saveMapSessionMechanics(
            String mapSessionId,
            List<NearbyMechanicDTO> mechanics
    ) {
         // this is become map-session:mechanics:user-map-28
        String sessionKey = "map-session:mechanics:" + mapSessionId;

        redisTemplate.delete(sessionKey);

        for (NearbyMechanicDTO mechanic : mechanics) {
            String mechanicId = mechanic.getMechanicId().toString();
                // har mechanci id nikal rha hay aur kuch is trah hoga ye
                //map-session:mechanics:user-map-28 iskey kay sary mechanci id jessay
            //map-session:mechanics:user-map-28 [55,57,59] mtlb kay user 28 kay 55 57 59 nearby mechancis hain


            redisTemplate.opsForSet().add(sessionKey, mechanicId);

            redisTemplate.opsForSet().add(
                    "mechanic:map-sessions:" + mechanicId,
                    mapSessionId
            );
        }
    }
    @Transactional
    public ResponseEntity<?> acceptRequest(Long requestId, String mechanicPhoneNumber) {
        Optional<RequestService> checkrequestservice = serviceRequestRepository.findById(requestId);

        Optional<Mechanic> mechanicOptional = mechanicRepository.findByPhonenumber(mechanicPhoneNumber);
        if (checkrequestservice.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Request Not Found");
        }
        if (mechanicOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Mechanic Not Found");
        }

        Mechanic mechanic = mechanicOptional.get();
        RequestService requestService = checkrequestservice.get();

        if (mechanic.isIsengaged()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Mechanic Already Engaged");
        }
//iskay ander nearby mechanics hain
        Set<String> nearbyMechanicsList =
                redisTemplate.opsForSet().members(
                        "request:mechanics:" + requestId
                );
        boolean flag = true ;
        for(String id : nearbyMechanicsList){
            System.out.println("This is my id: " + id);
            if(id.equals(mechanic.getId().toString())){
                flag = false ;

            }
        }
        if(flag){
            return  ResponseEntity.status(HttpStatus.CONFLICT).body("This is not your request "+ requestId);
        }
        int updatedRows = serviceRequestRepository.acceptRequest(requestId, mechanic);

        if (updatedRows == 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Request Already Accepted Or Not Available");
        }

        mechanic.setIsengaged(true);
        mechanicRepository.save(mechanic);

        AcceptedUserMechanicDto acceptedMechanicDto = buildAcceptedMechanicDto(
                mechanic,
                requestService
        );
// jab mechanic accept kraiga tu user ko mechanic ka data show hojaiga with distance and eta and vice verse
        simpMessagingTemplate.convertAndSend(
                "/topic/request/" + requestId,
                acceptedMechanicDto
        );

        List<String > ids =  new ArrayList<>();

        Map<String, Object> expirePayload = new HashMap<>();
        expirePayload.put("requestId", requestId);
        expirePayload.put("type", "ROAD_REQUEST_EXPIRED");
        expirePayload.put("message", "The request accepted by another mechanic "+ mechanic.getName());

        //check krnga phly ids null tu nh hay
        if (nearbyMechanicsList != null) {
       // phir may nearby mechanics ki ek ek kray k list nikalnga
            for (String mechanicId : nearbyMechanicsList) {

                if (!mechanicId.equals(mechanic.getId().toString())){
                    simpMessagingTemplate.convertAndSend(
                            "/topic/mechanic/requests/" + mechanicId,
                            (Object) expirePayload
                    );
                    ids.add(mechanicId);
                }
              }

        }
        System.out.println("ye hay mechanic jab request accept hogi tu inki pass say page gayab "+ ids);


        return ResponseEntity.ok(acceptedMechanicDto);
    }

    private AcceptedUserMechanicDto buildAcceptedMechanicDto(
            Mechanic mechanic,
            RequestService requestService
    ) {
        RoadInfo roadInfo = getAcceptedMechanicRoadInfo(mechanic, requestService);

        Double distance = null;
        String eta = null;
        if (roadInfo != null && roadInfo.getDistance() >= 0) {
            distance = roadInfo.getDistance();
            eta = roadInfo.getDistancetime();
        }

        Point mechanicPoint = getMechanicCurrentPoint(mechanic);

        return new AcceptedUserMechanicDto(
                requestService.getRequestId(),
                mechanic.getId(),
                requestService.getUser().getUserid() ,
                requestService.getUser().getUsername(),
                requestService.getUser().getUserimgurl() ,
                requestService.getLocationName() ,
                mechanic.getName(),
                mechanic.getPhonenumber(),
                mechanic.getMechanicimgurl(),
                mechanic.getAverageRating(),
                mechanic.getTotalReviews(),
                mechanic.getShopaddress(),
                mechanic.getMechanictype(),
                mechanic.getExperienceyears(),
                mechanicPoint == null ? null : mechanicPoint.getY(),
                mechanicPoint == null ? null : mechanicPoint.getX(),
                distance,
                eta,
                requestService.getUserLatitude(),
                requestService.getUserLongitude()
        );
    }

    private RoadInfo getAcceptedMechanicRoadInfo(
            Mechanic mechanic,
            RequestService requestService
    ) {
        Point mechanicPoint = getMechanicCurrentPoint(mechanic);
        if (mechanicPoint == null) {
            return null;
        }

        String destinationsParam =
                mechanicPoint.getY() + "," + mechanicPoint.getX();

        List<RoadInfo> roadDistances = new GoogleDistance().getBatchRoadDistances(
                requestService.getUserLatitude(),
                requestService.getUserLongitude(),
                destinationsParam
        );

        if (roadDistances.isEmpty()) {
            return null;
        }
        return roadDistances.get(0);
    }

    private Point getMechanicCurrentPoint(Mechanic mechanic) {
        List<Point> redisPositions = redisTemplate.opsForGeo()
                .position("mechanic", mechanic.getId().toString());

        if (redisPositions != null &&
                !redisPositions.isEmpty() &&
                redisPositions.get(0) != null) {
            return redisPositions.get(0);
        }

        if (mechanic.getLatitude() != null && mechanic.getLongitude() != null) {
            return new Point(
                    mechanic.getLongitude().doubleValue(),
                    mechanic.getLatitude().doubleValue()
            );
        }

        if (mechanic.getShoplatitude() != null &&
                mechanic.getShoplongitude() != null) {
            return new Point(
                    mechanic.getShoplongitude().doubleValue(),
                    mechanic.getShoplatitude().doubleValue()
            );
        }

        return null;
    }

    public ResponseEntity<?> getRequestTracking(Long requestId, String userPhoneNumber) {
        Optional<RequestService> requestOpt = serviceRequestRepository.findById(requestId);
        if (requestOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Request Not Found");
        }

        RequestService request = requestOpt.get();
        if (!request.getUser().getPhonenumber().equals(userPhoneNumber)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not allowed");
        }

        if (request.getMechanic() != null) {
            AcceptedUserMechanicDto dto = buildAcceptedMechanicDto(
                    request.getMechanic(),
                    request
            );
            dto.setRequestStatus(request.getRequestStatus().name());
            return ResponseEntity.ok(dto);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("requestId", request.getRequestId());
        payload.put("requestStatus", request.getRequestStatus().name());
        payload.put("serviceType", request.getServiceType());
        payload.put("userNotes", request.getUserNotes());
        payload.put("userLatitude", request.getUserLatitude());
        payload.put("userLongitude", request.getUserLongitude());
        payload.put("locationName", request.getLocationName());
        return ResponseEntity.ok(payload);
    }

    @Transactional
    public ResponseEntity<?> cancelRequest(Long requestId, String userPhoneNumber) {
        Optional<RequestService> requestOpt = serviceRequestRepository.findById(requestId);
        if (requestOpt.isEmpty()) return ResponseEntity.badRequest().body("Not Found");

        RequestService request = requestOpt.get();

        // Status update
        request.setRequestStatus(ServiceRequestStatus.CANCELLED);

        Map<String, Object> cancelPayload = new HashMap<>();
        cancelPayload.put("requestId", request.getRequestId());
        cancelPayload.put("type", "ROAD_REQUEST_CANCELLED");
        cancelPayload.put("message", "The user has cancelled the request.");

        if (request.getMechanic() == null) {
            System.out.println("Mechanic is null");
            // SCENARIO 1: BEFORE ACCEPT
            // Yahan par aap un mechanics ki list nikalenge jinhe message bheja tha
            // aur un sab ko loop chala kar cancel message bhej denge.
                List<String> ids = new ArrayList<>();
            Set<String> nearbyMechanicsList =
                    redisTemplate.opsForSet().members(
                            "request:mechanics:" + requestId
                    );
            if (nearbyMechanicsList != null) {

                for (String mechanicId : nearbyMechanicsList) {
                    ids.add(mechanicId);
                    simpMessagingTemplate.convertAndSend(
                            "/topic/mechanic/requests/" + mechanicId,
                            (Object) cancelPayload
                    );

                }
                redisTemplate.delete(
                        "request:mechanics:" + requestId
                );
            }

        }
        else {
            // SCENARIO 2: AFTER ACCEPT
            Mechanic assignedMechanic = request.getMechanic();
            assignedMechanic.setIsengaged(false); // Mechanic ko free kar diya
            mechanicRepository.save(assignedMechanic);

            // Sirf is ek mechanic ko message bhejain jo tracking map par hai
            simpMessagingTemplate.convertAndSend(
                    "/topic/request/" + request.getRequestId(),
                    (Object)  cancelPayload
            );
        }

        serviceRequestRepository.save(request);
        return ResponseEntity.ok("Request Cancelled");
    }

    public ResponseEntity<?> checkarrived(Long requestId, String phonenumber) {

        Optional<Mechanic> checkmechanic = mechanicRepository.findByPhonenumber(phonenumber);
        if(checkmechanic.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic Not Found");
        }
        Mechanic mechanic = checkmechanic.get();
        Optional<RequestService> requestOpt = serviceRequestRepository.findByRequestIdAndMechanic(requestId , mechanic);
        if(requestOpt.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Request with this mechanic" +mechanic.getId()+"Not Found");
        }
        RequestService request = requestOpt.get();

        double  userlatitude =  request.getUserLatitude();
        double  userlongitude = request.getUserLongitude() ;

        String etaKey = "request:eta:" + request.getRequestId();

        Object mechaniclatitude = redisTemplate.opsForHash().get(
                etaKey,
                "lastLat"

        );

        Object mechaniclongitude = redisTemplate.opsForHash().get(
                etaKey,
                "lastLng"

        );

        if(mechaniclatitude == null || mechaniclongitude == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Mechanic live location not available");
        }
        String mechaniclat = mechaniclatitude.toString();
        String mechaniclong = mechaniclongitude.toString();

        double mechLat = Double.parseDouble(mechaniclat);
        double mechLng = Double.parseDouble(mechaniclong);
        LiveLocationController locationController = new LiveLocationController();

        double distance = locationController.calculateHaversineMeters(
                userlatitude,
                userlongitude,
                mechLat,
                mechLng
        );

        System.out.println("Distance between user and mechanic: " + distance);
        //ager distance 80m say kam hay tu wo phnch gya hay mechanic user kay pass
        if(distance <= 80) {   // threshold
            request.setRequestStatus(ServiceRequestStatus.ARRIVED);
            serviceRequestRepository.save(request);

            return ResponseEntity.ok("ARRIVED");
        }



        System.out.println("this is user latitude: "+ userlatitude +" and this is longitude "+ userlongitude);
        System.out.println("this is user latitude: "+ mechaniclatitude +" and this is longitude "+ mechaniclongitude);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("NOT ARRIVED - mechanic is " + (int)distance + " meters away");

    }

    public ResponseEntity<?> sendfinalprice(SendPriceDto dto, String mechphonenumber) {

         Optional<Mechanic>  checkmechanic = mechanicRepository.findByPhonenumber(mechphonenumber) ;
         if (checkmechanic.isEmpty()) {
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic Not Found");
         }

    Mechanic mechanic = checkmechanic.get();
       Optional<RequestService>   checkrquest = serviceRequestRepository.findByRequestIdAndMechanic(dto.getRequestId() , mechanic);
        if(checkrquest.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Request Not Found");
        }

            RequestService request = checkrquest.get();
        if(! request.getRequestStatus().equals(ServiceRequestStatus.ARRIVED)){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request Status Not Arrived");
        }


        request.setRequestStatus(ServiceRequestStatus.WAITING_USER_APPROVAL);

        serviceRequestRepository.save(request);

        // 📡 notify user via websocket
        Map<String, Object> pricepayload = new HashMap<>();
        pricepayload.put("requestId", request.getRequestId());
        pricepayload.put("type", "FINAL_PRICE_SENT");
        pricepayload.put("finalPrice", dto.getFinalPrice());

        simpMessagingTemplate.convertAndSend(
                "/topic/request/" + request.getRequestId(),
                (Object)    pricepayload
        );


        return ResponseEntity.ok("Final price sent to "+ request.getUser().getUsername());


    }

    public ResponseEntity<?> approvepaymentrequest(SendPriceDto dto, String userphonenumber) {
        Optional<User> checkuser = userRepository.findByPhonenumber(userphonenumber);
        if(checkuser.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User Not Found");
        }
        User user =  checkuser.get();
        Optional<RequestService> checkrequest = serviceRequestRepository.findByRequestIdAndUser(dto.getRequestId()
        , user);
        if(checkrequest.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Request Not Found");
        }

        RequestService requestService = checkrequest.get();
        if(requestService.getRequestStatus().equals(ServiceRequestStatus.WAITING_USER_APPROVAL)){
          return  ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request Status Not Approved");
        }

        requestService.setRequestStatus(ServiceRequestStatus.WORK_STARTED);
        requestService.setInspectionPrice(dto.getFinalPrice());
        serviceRequestRepository.save(requestService);

        Map<String, Object> approvePayload = new HashMap<>();

        approvePayload.put("requestId", requestService.getRequestId());
        approvePayload.put("type", "USER_APPROVED");
        approvePayload.put("status", "WORK_STARTED");

        approvePayload.put("finalPrice", requestService.getInspectionPrice());

        approvePayload.put("message", "User approved the final price. Start work now.");

        approvePayload.put("userId", requestService.getUser().getUserid());
        approvePayload.put("mechanicId", requestService.getMechanic().getId());

        simpMessagingTemplate.convertAndSend(
                "/topic/request/" + requestService.getRequestId(),
                (Object)    approvePayload
        );
        return ResponseEntity.ok("Approved payment successfully");


    }
}
