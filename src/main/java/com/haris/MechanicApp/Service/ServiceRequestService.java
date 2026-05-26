package com.haris.MechanicApp.Service;

import com.haris.MechanicApp.Model.GoogleDistance;
import com.haris.MechanicApp.Model.Location.Location;
import com.haris.MechanicApp.Model.Location.LocationDTO;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Mechanic.NearbyMechanicDTO;
import com.haris.MechanicApp.Model.Mechanic.NearbyMechanicMapResponseDto;
import com.haris.MechanicApp.Model.RequestService.CreateServiceRequestDto;
import com.haris.MechanicApp.Model.RequestService.MechanicRequestNotificationDto;
import com.haris.MechanicApp.Model.RequestService.RequestService;
import com.haris.MechanicApp.Model.RequestService.ServiceRequestStatus;
import com.haris.MechanicApp.Model.RoadInfo;
import com.haris.MechanicApp.Model.Verification.User;
import com.haris.MechanicApp.Repository.MechanicRepository;
import com.haris.MechanicApp.Repository.ServiceRequestRepository;
import com.haris.MechanicApp.Repository.UserRepository;
import io.opentelemetry.api.common.Value;
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
        request.setRequestStatus(ServiceRequestStatus.PENDING);
        request.setPaymentStatus("UNPAID");

        RequestService savedRequest = serviceRequestRepository.save(request);

       return ResponseEntity.ok(sendRequestToNearbyOnlineMechanics(savedRequest)) ;

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
                            request.getRequestStatus()
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

        Optional<Mechanic> mechanicOptional = mechanicRepository.findByPhonenumber(mechanicPhoneNumber);

        if (mechanicOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Mechanic Not Found");
        }

        Mechanic mechanic = mechanicOptional.get();

        if (mechanic.isIsengaged()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Mechanic Already Engaged");
        }

        int updatedRows = serviceRequestRepository.acceptRequest(requestId, mechanic);

        if (updatedRows == 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Request Already Accepted Or Not Available");
        }

        mechanic.setIsengaged(true);
        mechanicRepository.save(mechanic);

        simpMessagingTemplate.convertAndSend(
                "/topic/request/" + requestId,
                mechanic
        );

        return ResponseEntity.ok("Request Accepted Successfully");
    }
}