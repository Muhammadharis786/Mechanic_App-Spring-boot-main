package com.haris.MechanicApp.Service;

import com.haris.MechanicApp.Model.Appointments.*;
import com.haris.MechanicApp.Model.GoogleDistance;
import com.haris.MechanicApp.Model.Location.Location;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Mechanic.MechanicDTO;
import com.haris.MechanicApp.Model.Notification.MechanicNotificationDto;
import com.haris.MechanicApp.Model.Notification.Notification;
import com.haris.MechanicApp.Model.Notification.NotificationType;
import com.haris.MechanicApp.Model.Notification.UserNotificationDto;
import com.haris.MechanicApp.Model.Payment.PaymentStatus;
import com.haris.MechanicApp.Model.RequestService.SendPriceDto;
import com.haris.MechanicApp.Model.RoadInfo;
import com.haris.MechanicApp.Model.Verification.User;
import com.haris.MechanicApp.Repository.*;
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
public class AppointmentService {

    @Autowired
    UserRepository  userRepo;
    @Autowired
     private MechanicRepository mechanicrepo;
    @Autowired
    private AppointmentRepository  appointmentRepository;
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    private MechanicNotificationRepository notificationRepository;
    @Autowired
    private RedisTemplate<String , String > redisTemplate;
    @Autowired
    private AppointmentRequestRepository appointmentRequestRepository ;
    @Autowired
    private FcmService fcmService;


    public String modernDate() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy h:mm a", Locale.ENGLISH);
        String formattedDate = now.format(formatter).toLowerCase();

        formattedDate = formattedDate.substring(0, 2) +
                formattedDate.substring(2, 3).toUpperCase() +
                formattedDate.substring(3);

        return formattedDate;
    }


//this is call when user click auto book appointment
    public ResponseEntity<?> autobookappointment(String userphonenumber  ,
                                             AutoAppointmentDto appointmentDto) {

        Optional<User> checkuser = userRepo.findByPhonenumber(userphonenumber);
        if(checkuser.isPresent()){

            User user=  checkuser.get();
            Appointments appointments = new Appointments();
            appointments.setUser(user);

            appointments.setAppointmentDate(appointmentDto.getAppointmentDate());
            appointments.setAppointmentTime(appointmentDto.getAppointmentTime());
            appointments.setProblemDescription(appointmentDto.getProblemDescription());
            appointments.setCreatedAt(Instant.now());
            appointments.setServiceType(appointmentDto.getServiceType());
            appointments.setLatitude(appointmentDto.getLatitude());
            appointments.setLongitude(appointmentDto.getLongitude());
            appointments.setAddress(appointmentDto.getAddress());
            appointments.setStatus(AppointmentStatus.PENDING);
            String mechanictyperequest = appointmentDto.getServiceType().toUpperCase();
             mechanictyperequest=  mechanictyperequest.split(" ")[0];
            appointmentRepository.save(appointments);





            double  userlongitude  = appointmentDto.getLongitude().doubleValue();
            double  userlatitude  = appointmentDto.getLatitude().doubleValue();

            GeoOperations<String  , String> geoOperations = redisTemplate.opsForGeo();
            GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                    geoOperations.search(
                            "mech",
                            GeoReference.fromCoordinate(userlongitude, userlatitude),
                            new Distance(65, Metrics.KILOMETERS),
                            RedisGeoCommands.GeoSearchCommandArgs
                                    .newGeoSearchArgs()
                                    .includeDistance()
                                    .includeCoordinates()

                    );
            List<Long> mechanicIds = new ArrayList<>();
            int count =0 ;
            for (GeoResult<RedisGeoCommands.GeoLocation<String>> result  : results){
                long mechanicid =  Long.parseLong (result.getContent().getName());
                 String mechanictype =   (String) redisTemplate.opsForHash()
                            .get("mechanic:details:" + mechanicid, "serviceType");

                // ✅ Sirf wahi mechanic add karo jiska service type match kare
                if(mechanictyperequest.equalsIgnoreCase(mechanictype)){
                    mechanicIds.add(mechanicid);
                    count++;
                }
            }

            System.out.println(count + " mechanics mil gay hain" +  mechanicIds);
            if(mechanicIds.isEmpty()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("In your area  "+ mechanictyperequest + " mechanic not available");
            }

            List<Mechanic> mechanics = mechanicrepo.findAllById(mechanicIds);
            if (mechanics.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanics nh milay tu notifcation kessay jainga" );
            }


            List<BookingNotificationDto> notidto = new ArrayList<>();

            for (Mechanic mechanic : mechanics){
            long mechanicId = mechanic.getId();
                // 🔥 1. CREATE APPOINTMENT REQUEST (IMPORTANT)
                AppointmentRequest request = new AppointmentRequest();
                request.setAppointment(appointments);
                request.setMechanic(mechanic);
                request.setStatus(RequestStatus.PENDING);
                request.setCreatedAt(Instant.now());

                appointmentRequestRepository.save(request);


                Notification notification =  new Notification();
                notification.setAppointments(appointments);
                notification.setMechanic(mechanic);
                notification.setMessage("You received a new appointment request");
                notification.setTitle("New Booking");
                notification.setUser(appointments.getUser());
                notification.setType(NotificationType.APPOINTMENT_REQUEST);
                notification.setCreatedAt(Instant.now());
                notificationRepository.save(notification);




                //ye dto bnay ahay mechanic ko notication bhjnay k lie
                BookingNotificationDto dto =  new BookingNotificationDto()  ;
                dto.setAddress(appointmentDto.getAddress());
                dto.setLatitude(appointmentDto.getLatitude());
                dto.setLongitude(appointmentDto.getLongitude());
                dto.setServiceType(appointmentDto.getServiceType());
                dto.setAppointmentTime(appointmentDto.getAppointmentTime());
                dto.setAppointmentDate(appointmentDto.getAppointmentDate());
                dto.setProblemDescription(appointmentDto.getProblemDescription());
                dto.setUserphonenumber(user.getPhonenumber());
                dto.setUserimage(user.getUserimgurl());
                dto.setUsername(user.getUsername());
                dto.setMechshoplat(mechanic.getShoplatitude());
                dto.setCreated_at(notification.getCreatedAt());
                dto.setMechshoplong(mechanic.getShoplongitude());



                dto.setMechname(mechanic.getName());
                dto.setMechexperience(mechanic.getExperienceyears());
                dto.setMechimage(mechanic.getMechanicimgurl());
                dto.setMechtype(mechanic.getMechanictype());
                dto.setTotalreviews(mechanic.getTotalReviews());
                dto.setMechrating(mechanic.getAverageRating());
                dto.setAppointmentid(appointments.getAppointmentId());
                dto.setNotificationid(notification.getId());
                dto.setVisitcharge(appointments.getVisitingCharge());

                String destination = "/topic/bookappointment/nearbymechanics/" + mechanicId;
                simpMessagingTemplate.convertAndSend(destination,dto);
                notidto.add(dto);
            //THIS IS FOR FIREBASE FCM NOTIFCATION JO MOBILE KAY SYSTEM PER SHOW HOGA APPLICATION K BAHIR
                Map<String, String> fcmData = new HashMap<>();
                fcmData.put("type",NotificationType.APPOINTMENT_REQUEST.toString());
                fcmData.put("appointmentId", appointments.getAppointmentId());
                fcmData.put("notificationId", String.valueOf(notification.getId()));

                fcmService.sendToMechanic(
                        mechanic,
                        "New Booking",
                        "You received a new appointment request",
                        fcmData
                );

            }

            return ResponseEntity.status(200).body(appointments.getAppointmentId());



        }
        return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found");
    }

    public ResponseEntity<?> nearbymechanics(String userphonenumber, Location location) {
        Optional<User>  checkuser = userRepo.findByPhonenumber(userphonenumber);

        if(checkuser.isPresent()) {
            System.out.println(location.getLongitude() +  " and : " + location.getLatitude());
            double  userlongitude  = location.getLongitude().doubleValue();
            double  userlatitude  = location.getLatitude().doubleValue();
            GeoOperations<String  , String> geoOperations = redisTemplate.opsForGeo();
            StringBuilder destinationsparam = new StringBuilder();
            GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                    geoOperations.search(
                            "mech",
                            GeoReference.fromCoordinate(userlongitude, userlatitude),
                            new Distance(65, Metrics.KILOMETERS),
                            RedisGeoCommands.GeoSearchCommandArgs
                                    .newGeoSearchArgs()
                                    .includeDistance()
                                    .includeCoordinates()

                    );
            List<Long> mechanicIds = new ArrayList<>();

            for (GeoResult<RedisGeoCommands.GeoLocation<String>> result  : results){
                long mechanicid =  Long.parseLong (result.getContent().getName());
                Point   point = result.getContent().getPoint();
                destinationsparam.append(point.getY()).append(",").append(point.getX()).append("|");
                    mechanicIds.add(mechanicid);

            }
            if(!destinationsparam.isEmpty()){
                destinationsparam.setLength(destinationsparam.length()-1);
            }
            GoogleDistance googleapi = new GoogleDistance();
            List<RoadInfo> roadDistances = googleapi.getBatchRoadDistances(
                    userlatitude, userlongitude, destinationsparam.toString()
            );
            List<MechanicDTO> mechanicDTOs = new ArrayList<>();

            List<Mechanic> allnearbymechanics = mechanicrepo.findAllById(mechanicIds);
            if(allnearbymechanics.isEmpty()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanics not available");
            }

            Map<Long, Double> distanceMap = new HashMap<>();
            for (int i = 0; i < mechanicIds.size(); i++) {
                distanceMap.put(mechanicIds.get(i), roadDistances.get(i).getDistance());
            }
        for (Mechanic mechanic : allnearbymechanics){
            MechanicDTO dto = new MechanicDTO();
            dto.setDistance(BigDecimal.valueOf(distanceMap.get(mechanic.getId())));
            dto.setName(mechanic.getName());
            dto.setId(mechanic.getId());
            dto.setAveragerating(mechanic.getAverageRating());
            dto.setMechanicimgurl(mechanic.getMechanicimgurl());
            dto.setExperience(mechanic.getExperienceyears());
            dto.setMechanicType(mechanic.getMechanictype());
            dto.setPhonenumber(mechanic.getPhonenumber());
            dto.setLatitude(mechanic.getLatitude());
            dto.setLongitude(mechanic.getLongitude());

            mechanicDTOs.add(dto);
        }

            return ResponseEntity.ok(mechanicDTOs);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found");
        }

    public ResponseEntity<?> manualbookappointment(String userphonenumber,
                                                   ManualAppointmentDto appointmentDto) {

        Optional<User> checkuser = userRepo.findByPhonenumber(userphonenumber);
        Optional<Mechanic> checkmechanic = mechanicrepo.findById(appointmentDto.getId());

        if (checkuser.isEmpty() || checkmechanic.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User or Mechanic Not Found");
        }

        User user = checkuser.get();
        Mechanic mechanic = checkmechanic.get();

        // ================================
        // STEP 1: CREATE APPOINTMENT
        // ================================
        Appointments appointment = new Appointments();
        appointment.setUser(user);
        appointment.setMechanic(mechanic);
        appointment.setAppointmentDate(appointmentDto.getAppointmentDate());
        appointment.setAppointmentTime(appointmentDto.getAppointmentTime());
        appointment.setProblemDescription(appointmentDto.getProblemDescription());
        appointment.setCreatedAt(Instant.now());
        appointment.setServiceType(appointmentDto.getServiceType());
        appointment.setLatitude(appointmentDto.getLatitude());
        appointment.setLongitude(appointmentDto.getLongitude());
        appointment.setAddress(appointmentDto.getAddress());
        appointment.setStatus(AppointmentStatus.PENDING);

        appointmentRepository.save(appointment);

        // ================================
        // STEP 2: CREATE SINGLE REQUEST
        // ================================
        AppointmentRequest request = new AppointmentRequest();
        request.setAppointment(appointment);
        request.setMechanic(mechanic);
        request.setStatus(RequestStatus.PENDING);
        request.setCreatedAt(Instant.now());

        appointmentRequestRepository.save(request);

        // ================================
        // STEP 3: NOTIFICATION
        // ================================
        Notification notification = new Notification();
        notification.setAppointments(appointment);
        notification.setMechanic(mechanic);
        notification.setUser(user);
        notification.setMessage("You received a new appointment request");
        notification.setTitle("New Booking");
        notification.setType(NotificationType.APPOINTMENT_REQUEST);
        notification.setCreatedAt(Instant.now());

        notificationRepository.save(notification);

        // ================================
        // STEP 4: WEBSOCKET DTO
        // ================================
        BookingNotificationDto dto = new BookingNotificationDto();

        dto.setAppointmentid(appointment.getAppointmentId());
        dto.setNotificationid(notification.getId());

        dto.setAddress(appointmentDto.getAddress());
        dto.setLatitude(appointmentDto.getLatitude());
        dto.setLongitude(appointmentDto.getLongitude());
        dto.setServiceType(appointmentDto.getServiceType());
        dto.setAppointmentDate(appointmentDto.getAppointmentDate());
        dto.setAppointmentTime(appointmentDto.getAppointmentTime());
        dto.setProblemDescription(appointmentDto.getProblemDescription());

        dto.setUserphonenumber(user.getPhonenumber());
        dto.setUsername(user.getUsername());
        dto.setUserimage(user.getUserimgurl());

        dto.setMechname(mechanic.getName());
        dto.setMechimage(mechanic.getMechanicimgurl());
        dto.setMechtype(mechanic.getMechanictype());
        dto.setMechexperience(mechanic.getExperienceyears());
        dto.setMechrating(mechanic.getAverageRating());
        dto.setTotalreviews(mechanic.getTotalReviews());

        dto.setMechshoplat(mechanic.getShoplatitude());
        dto.setMechshoplong(mechanic.getShoplongitude());

        dto.setVisitcharge(appointment.getVisitingCharge());
        dto.setCreated_at(notification.getCreatedAt());

        // ================================
        // STEP 5: SEND REALTIME EVENT
        // ================================
        String destination =
                "/topic/bookappointment/nearbymechanics/" + mechanic.getId();

        simpMessagingTemplate.convertAndSend(destination, dto);
        Map<String, String> fcmData = new HashMap<>();
        fcmData.put("type", NotificationType.APPOINTMENT_REQUEST.toString());
        fcmData.put("appointmentId", appointment.getAppointmentId());
        fcmData.put("notificationId", String.valueOf(notification.getId()));

        fcmService.sendToMechanic(
                mechanic,
                "New Booking",
                "You received a new appointment request",
                fcmData
        );

        return ResponseEntity.ok(appointment.getAppointmentId());
    }

    public ResponseEntity<?> mechanicallnotifications(String phonenumber) {
        Optional<Mechanic> checkmechanic = mechanicrepo.findByPhonenumber(phonenumber);
        if(checkmechanic.isPresent()) {
            Mechanic mechanic =  checkmechanic.get();
            List<MechanicNotificationDto> bookingNotificationDtos = new   ArrayList<>();
            List<NotificationType> notificationTypes = new ArrayList<>();
            notificationTypes.add(NotificationType.APPOINTMENT_REQUEST);
            notificationTypes.add(NotificationType.APPOINTMENT_CANCELLED);
            notificationTypes.add(NotificationType.APPOINTMENT_COMPLETED);
               List<Notification> allnotifications =  notificationRepository.findByMechanic_IdAndTypeInOrderByCreatedAtDesc(mechanic.getId() ,notificationTypes);

               for (Notification notification : allnotifications) {
                   MechanicNotificationDto dto = new MechanicNotificationDto() ;
                    dto .setNotificationId(notification.getId());
                    dto.setRead(notification.isRead());
                    dto.setType(notification.getType());
                    dto.setMessage(notification.getMessage());
                    if(notification.getAppointments()!=null){
                        dto.setAppointmentId(notification.getAppointments().getAppointmentId());
                    }

                    dto.setCreatedAt(notification.getCreatedAt());
                   bookingNotificationDtos.add(dto);
  }

                    return  ResponseEntity.ok( bookingNotificationDtos);
        }
        return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic not found");
    }

    public ResponseEntity<?> userallnotifications(String phonenumber) {
        Optional<User> checkuser = userRepo.findByPhonenumber(phonenumber);
        if (checkuser.isPresent()) {
            User user = checkuser.get();
            List<UserNotificationDto> notificationDtos = new ArrayList<>();

            List<MechanicNotificationDto> bookingNotificationDtos = new   ArrayList<>();
            List<NotificationType> notificationTypes = new ArrayList<>();
            notificationTypes.add(NotificationType.APPOINTMENT_ACCEPTED);
            notificationTypes.add(NotificationType.APPOINTMENT_REJECTED);
            notificationTypes.add(NotificationType.APPOINTMENT_EXPIRED);
            notificationTypes.add(NotificationType.MECHANIC_ON_THE_WAY);
            notificationTypes.add(NotificationType.APPOINTMENT_COMPLETED);


            List<Notification> allnotifications =
                    notificationRepository.findByUser_UseridAndTypeInOrderByCreatedAtDesc(user.getUserid() , notificationTypes);

            for (Notification notification : allnotifications) {
                UserNotificationDto dto = new UserNotificationDto();
                dto.setNotificationid(notification.getId());
                dto.setAppointmentid(notification.getAppointments().getAppointmentId());
                dto.setType(notification.getType());
                dto.setMessage(notification.getMessage());
                dto.setIsread(notification.isRead());
                dto.setCreated_at(notification.getCreatedAt());
                notificationDtos.add(dto);
            }

            return ResponseEntity.ok(notificationDtos);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
    }
    public void isreadnotificationuser(String userphonenumber, long notificationid) {
        Optional<User>  checkuser= userRepo.findByPhonenumber(userphonenumber);

        if(checkuser.isPresent()) {
            User user = checkuser.get();
            Optional<Notification> notification = notificationRepository.findByIdAndUser(notificationid,user);
            if(notification.isPresent()) {
                Notification mechanicnotification = notification.get();
                mechanicnotification.setRead(true);
                notificationRepository.save(mechanicnotification);
                System.out.println("may read hogya hn");

            }


        }

    }

    public void isreadnotification(String userphonenumber, long notificationid) {
        Optional<Mechanic> ismechanic = mechanicrepo.findByPhonenumber(userphonenumber);

        if(ismechanic.isPresent()) {
            Mechanic mechanic = ismechanic.get();
            Optional<Notification> notification = notificationRepository.findByIdAndMechanic(notificationid,mechanic);
            if(notification.isPresent()) {
                Notification mechanicnotification = notification.get();
                mechanicnotification.setRead(true);
                notificationRepository.save(mechanicnotification);
                System.out.println("may read hogya hn");

            }

        }

    }

    public ResponseEntity<?> showuserappointments(String userphonenumber) {
        Optional<User> checkuser = userRepo.findByPhonenumber(userphonenumber);
        if(checkuser.isPresent()) {
            User user = checkuser.get();
            List<Appointments> usersappointments = appointmentRepository.findByUser(user);
            List<UserAppointmentDto>listofappointments =  new ArrayList<>();
            if(usersappointments.isEmpty()) {
                return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("No Appointments ");
            }
            for(Appointments appointments : usersappointments) {
                UserAppointmentDto dto = new UserAppointmentDto();

                // 1. Common Details (Jo dono cases mein hongi)
                dto.setAppointmentid(appointments.getAppointmentId());
                //this is user address wo address hay jha per isko service chie
                dto.setAddress(appointments.getAddress());
                dto.setStatus(appointments.getStatus());
                dto.setAppointmentDate(appointments.getAppointmentDate());
                dto.setAppointmentTime(appointments.getAppointmentTime());
                dto.setProblemDescription(appointments.getProblemDescription());
                dto.setLatitude(appointments.getLatitude());
                dto.setLongitude(appointments.getLongitude());
                dto.setServiceType(appointments.getServiceType());


                // Visiting charge hamesha dikhayein
                dto.setReason(appointments.getReason());


                // 2. Mechanic Details (Sirf tab jab mechanic assigned ho)
                if(appointments.getMechanic() != null) {
                    dto.setMechanicid(appointments.getMechanic().getId());
                    dto.setMechimage(appointments.getMechanic().getMechanicimgurl());
                    dto.setMechname(appointments.getMechanic().getName());
                    dto.setMechexperience(appointments.getMechanic().getExperienceyears());
                    dto.setMechrating(appointments.getMechanic().getAverageRating());
                    dto.setMechanicshopaddress(appointments.getMechanic().getShopaddress());
                    dto.setMechshoplat(appointments.getMechanic().getShoplatitude());
                    dto.setMechshoplong(appointments.getMechanic().getShoplongitude());
                    dto.setMechnumber(appointments.getMechanic().getPhonenumber());
                    if(appointments.getPaymentStatus()!=null){
                        dto.setPaymentStatus(appointments.getPaymentStatus().toString());

                    }
                    if(appointments.getVisitingCharge()!=null && appointments.getRepairAmount()!=null
                            && appointments.getAmount()!=null ){
                        dto.setVisitingcharges(appointments.getVisitingCharge());
                        dto.setAmount(appointments.getAmount());
                        dto.setRepairAmount(appointments.getRepairAmount());
                    }
                    dto.setReason(appointments.getReason());

                }


                // 3. List mein sirf EK BAAR add karein
                listofappointments.add(dto);
            }


            return  ResponseEntity.ok(listofappointments);
            }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User nh mila");
        }

    public ResponseEntity<?> showmechanicappointments(String mechphonenumber) {

        Optional<Mechanic> checkmechanic = mechanicrepo.findByPhonenumber(mechphonenumber);
        if (checkmechanic.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic nahi mila");
        }

        Mechanic mechanic = checkmechanic.get();
        List<MechanicAppointmentDTO> listofappointments = new ArrayList<>();

        // =============================================
        // PART 1: Pending Requests (abhi accept/reject nahi kiya)
        // AppointmentRequest table se → status = PENDING
        // =============================================
        List<AppointmentRequest> pendingRequests =
                appointmentRequestRepository.findByMechanicAndStatus(mechanic, RequestStatus.PENDING);

        for (AppointmentRequest req : pendingRequests) {
            Appointments apt = req.getAppointment(); // parent appointment

            MechanicAppointmentDTO dto = new MechanicAppointmentDTO();
            dto.setAppointmentid(apt.getAppointmentId());
            dto.setStatus(apt.getStatus().toString()); // frontend ko pata chale ye pending request hai
            dto.setAppointmentDate(apt.getAppointmentDate());
            dto.setAppointmentTime(apt.getAppointmentTime());
            dto.setProblemDescription(apt.getProblemDescription());
            dto.setLatitude(apt.getLatitude());
            dto.setLongitude(apt.getLongitude());
            dto.setServiceType(apt.getServiceType());
            dto.setCreated_at(req.getCreatedAt());
            dto.setVisitingcharges(apt.getVisitingCharge());
            dto.setUseraddress(apt.getAddress());
            dto.setUsername(apt.getUser().getUsername());
            dto.setUserimage(apt.getUser().getUserimgurl());
            dto.setUserphonenumber(apt.getUser().getPhonenumber());
            dto.setMechshoplat(mechanic.getShoplatitude());
            dto.setMechshoplong(mechanic.getShoplongitude());


            listofappointments.add(dto);
        }

        // =============================================
        // PART 2: Accepted/Active Appointments
        // Appointments table se → mechanic = this mechanic
        // (PENDING, ACCEPTED, WORK_STARTED, COMPLETED, CANCELLED, REJECTED)
        // =============================================
        List<Appointments> myAppointments = appointmentRepository.findByMechanic(mechanic);

        for (Appointments apt : myAppointments) {
            if(apt.getStatus()!=AppointmentStatus.PENDING){
                MechanicAppointmentDTO dto = new MechanicAppointmentDTO();
                dto.setAppointmentid(apt.getAppointmentId());
                dto.setStatus(apt.getStatus().toString());
                dto.setAppointmentDate(apt.getAppointmentDate());
                dto.setAppointmentTime(apt.getAppointmentTime());
                dto.setProblemDescription(apt.getProblemDescription());
                dto.setLatitude(apt.getLatitude());
                dto.setLongitude(apt.getLongitude());
                dto.setServiceType(apt.getServiceType());
                dto.setCreated_at(apt.getCreatedAt());
                dto.setReason(apt.getReason());
                if(apt.getPaymentStatus()!=null){
                    dto.setPaymentStatus(apt.getPaymentStatus().toString());

                }
                dto.setUseraddress(apt.getAddress());
                dto.setUsername(apt.getUser().getUsername());
                dto.setUserimage(apt.getUser().getUserimgurl());
                dto.setUserphonenumber(apt.getUser().getPhonenumber());
                dto.setMechshoplat(mechanic.getShoplatitude());
                dto.setMechshoplong(mechanic.getShoplongitude());

                if(apt.getVisitingCharge()!=null && apt.getRepairAmount()!=null  && apt.getAmount()!=null ){
                    dto.setVisitingcharges(apt.getVisitingCharge());
                    dto.setAmount(apt.getAmount());
                    dto.setRepairAmount(apt.getRepairAmount());
                }

                listofappointments.add(dto);
            }

        }

        if (listofappointments.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Koi appointments nahi");
        }

        return ResponseEntity.ok(listofappointments);
    }
    // =========================================================
    // REJECT APPOINTMENT (CLEAN VERSION)
    // =========================================================
    public ResponseEntity<?> rejectappointment(
            String phonenumber,
            String appointmentid,
            ReasonDTO reasonDTO
    ) {

        Optional<Mechanic> mechOpt = mechanicrepo.findByPhonenumber(phonenumber);
        Optional<Appointments> appOpt = appointmentRepository.findByAppointmentId(appointmentid);

        if (mechOpt.isEmpty() || appOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Mechanic or Appointment not found");
        }

        Mechanic mechanic = mechOpt.get();
        Appointments appointment = appOpt.get();


        AppointmentRequest request = appointmentRequestRepository
                .findByMechanicAndAppointment(mechanic, appointment)
                .orElse(null);

        if (request == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Request not found for this mechanic");
        }

        // ================================
        // STEP 1: Reject THIS request only
        // ================================
        if(request.getStatus()!=RequestStatus.PENDING){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Action not allowed. Current status: " + request.getStatus());
        }

            request.setStatus(RequestStatus.REJECTED);
            request.setReason(reasonDTO.getReason());
            request.setRespondedAt(Instant.now());
            appointmentRequestRepository.save(request);


        // ================================
        // STEP 2: Fetch all requests of this appointment
        // ================================
        List<AppointmentRequest> allRequests =
                appointmentRequestRepository.findByAppointment(appointment);


        boolean anyAccepted = allRequests.stream()
                .anyMatch(r -> r.getStatus() == RequestStatus.ACCEPTED);
        if (anyAccepted) {
            return ResponseEntity.ok("Already accepted by another mechanic");
        }
        // ⚠️ If already accepted by someone → stop




        boolean allRejected = allRequests.stream()
                .allMatch(r -> r.getStatus() == RequestStatus.REJECTED);

        // ================================
        // STEP 3: FINAL STATE DECISION
        // ================================
        if (allRejected) {

            appointment.setStatus(AppointmentStatus.REJECTED);
            appointment.setReason("No mechanic accepted this request");
            appointmentRepository.save(appointment);

            // ================================
            // FINAL USER NOTIFICATION ONLY
            // ================================
            Notification notification = new Notification();
            notification.setType(NotificationType.APPOINTMENT_REJECTED);
            notification.setAppointments(appointment);
            notification.setTitle("Appointment Rejected");
            notification.setMessage("No mechanic accepted your request");
            notification.setMechanic(null); // optional (because it's final system state)
            notification.setUser(appointment.getUser());
            notification.setCreatedAt(Instant.now());
            notificationRepository.save(notification);

            AppointmentResponseDTO dto = new AppointmentResponseDTO();
            dto.setImage(null);
            dto.setMessage("No mechanic accepted your appointment");
            dto.setTitle("Final Rejection");
            dto.setCreatedAt(Instant.now());

            simpMessagingTemplate.convertAndSend(
                    "/topic/appointment/final-reject/" + appointment.getUser().getUserid(),
                    dto
            );
            Map<String, String> fcmData = new HashMap<>();
            fcmData.put("type", NotificationType.APPOINTMENT_REJECTED .toString());
            fcmData.put("appointmentId", appointment.getAppointmentId());
            fcmData.put("notificationId", String.valueOf(notification.getId()));

            fcmService.sendToUser(
                    appointment.getUser(),
                    "Appointment Rejected",
                    "No mechanic accepted your request",
                    fcmData
            );
            System.out.println("Ab gya hay notifcation");
        }

        // ================================
        // STEP 4: Optional mechanic-side update (no spam)
        // ================================
        AppointmentResponseDTO mechDto = new AppointmentResponseDTO();
        mechDto.setImage(mechanic.getMechanicimgurl());
        mechDto.setMessage(reasonDTO.getReason());
        mechDto.setTitle("Rejected");
        mechDto.setCreatedAt(Instant.now());

        simpMessagingTemplate.convertAndSend(
                "/topic/appointment/mechanic-update/" + mechanic.getId(),
                mechDto
        );

        return ResponseEntity.ok("Rejected Successfully");
    }
    public ResponseEntity<?> cancelappointment(
            String userphonenumber,
            String appointmentid,
            ReasonDTO reasonDTO
    ) {

        // =====================================
        // STEP 1: USER CHECK
        // =====================================
        Optional<User> checkuser = userRepo.findByPhonenumber(userphonenumber);

        if (checkuser.isEmpty()) {return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User Not Found");
        }

        User user = checkuser.get();

        // =====================================
        // STEP 2: FIND USER APPOINTMENT
        // =====================================
        Optional<Appointments> checkappointment = appointmentRepository.findByAppointmentIdAndUser(appointmentid, user);

        if (checkappointment.isEmpty()) {return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No appointment found with id " + appointmentid);
        }

        Appointments appointment = checkappointment.get();

        // =====================================
        // STEP 3: STATUS VALIDATION
        // only pending or accepted allowed
        // =====================================
        if (appointment.getStatus() != AppointmentStatus.PENDING && appointment.getStatus() != AppointmentStatus.ACCEPTED) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Action not allowed. Current status: " + appointment.getStatus());
        }

        // =====================================
        // STEP 4: UPDATE APPOINTMENT
        // =====================================
        appointment.setStatus(AppointmentStatus.CANCELLED);

        appointment.setReason(reasonDTO.getReason());

        appointmentRepository.save(appointment);

        // =====================================
        // STEP 5: GET ALL REQUESTS
        // =====================================
        List<AppointmentRequest> requests = appointmentRequestRepository.findByAppointment(appointment);
        if (requests.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No appointment requests found");
        }

        // =====================================
        // STEP 6: CANCEL REQUESTS
        // + NOTIFICATION
        // + WEBSOCKET
        // =====================================
        for (AppointmentRequest request : requests) {

            // sirf active requests cancel karo
            if (request.getStatus() == RequestStatus.PENDING || request.getStatus() == RequestStatus.ACCEPTED) {

                request.setStatus(RequestStatus.CANCELLED);

                request.setReason(reasonDTO.getReason());

                request.setRespondedAt(Instant.now());

                appointmentRequestRepository.save(request);
            }

            // =====================================
            // CREATE NOTIFICATION
            // =====================================
            Notification notification = new Notification();

            notification.setMessage(reasonDTO.getReason());

            notification.setTitle("Appointment Cancelled");

            notification.setType(NotificationType.APPOINTMENT_CANCELLED);

            notification.setAppointments(appointment);

            notification.setUser(user);

            notification.setMechanic(request.getMechanic());

            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);

            // =====================================
            // REALTIME SOCKET EVENT
            // =====================================
            AppointmentResponseDTO dto = new AppointmentResponseDTO();

            dto.setImage(user.getUserimgurl());

            dto.setMessage(reasonDTO.getReason());

            dto.setTitle("Appointment Cancelled");

            dto.setCreatedAt(Instant.now());

            String destination = "/topic/appointment/cancelappointment/" + request.getMechanic().getId();

            simpMessagingTemplate.convertAndSend(destination, dto);
            Map<String, String> fcmData = new HashMap<>();
            fcmData.put("type", NotificationType.APPOINTMENT_CANCELLED.toString());
            fcmData.put("appointmentId", appointment.getAppointmentId());
            fcmData.put("notificationId", String.valueOf(notification.getId()));

            fcmService.sendToMechanic(
                    request.getMechanic(),
                    "Appointment Cancelled",
                    reasonDTO.getReason(),
                    fcmData
            );
        }

        // =====================================
        // FINAL RESPONSE
        // =====================================
        return ResponseEntity.ok("Appointment Cancelled Successfully");
    }

    public ResponseEntity<?> acceptappointment(String phonenumber, String appointmentid) {

        Optional<Mechanic> checkmechanic = mechanicrepo.findByPhonenumber(phonenumber);
        Optional<Appointments> checkappointments = appointmentRepository.findByAppointmentId(appointmentid);

        if (checkmechanic.isEmpty() || checkappointments.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Mechanic or Appointment not found");
        }

        Mechanic mechanic = checkmechanic.get();
        Appointments appointment = checkappointments.get();

        // already accepted check



        Optional<AppointmentRequest> checkrequest =
                appointmentRequestRepository.findByMechanicAndAppointment(mechanic, appointment);

        if (checkrequest.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("You don't have request for appointment " + appointmentid);
        }

        AppointmentRequest request = checkrequest.get();

        if (request.getStatus() != RequestStatus.PENDING) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Appointment status is not pending");
        }

        // ================= ACCEPT REQUEST =================
        request.setStatus(RequestStatus.ACCEPTED);
        request.setRespondedAt(Instant.now());
        request.setAcceptedByMechanicId(mechanic.getId());
        appointmentRequestRepository.save(request);

        // ================= UPDATE APPOINTMENT =================
        appointment.setStatus(AppointmentStatus.ACCEPTED);
        appointment.setMechanic(mechanic);
        appointmentRepository.save(appointment);

        // ================= EXPIRE OTHER REQUESTS =================
        List<AppointmentRequest> allRequests =
                appointmentRequestRepository.findByAppointment(appointment);

        for (AppointmentRequest req : allRequests) {

            if (req.getMechanic().getId().equals(mechanic.getId()) ||
                    req.getStatus() != RequestStatus.PENDING) {
                continue;
            }

            req.setStatus(RequestStatus.EXPIRED);
            req.setReason("Appointment accepted by " + mechanic.getName());
            req.setRespondedAt(Instant.now());
            req.setAcceptedByMechanicId(mechanic.getId());
            appointmentRequestRepository.save(req);

            Notification notification = new Notification();
            notification.setType(NotificationType.APPOINTMENT_EXPIRED);
            notification.setAppointments(appointment);
            notification.setTitle("Appointment Expired");
            notification.setMessage("Appointment accepted by " + mechanic.getName());
            notification.setMechanic(req.getMechanic());
            notification.setCreatedAt(Instant.now());
            notificationRepository.save(notification);

            AppointmentResponseDTO dto = new AppointmentResponseDTO();
            dto.setImage(req.getMechanic().getMechanicimgurl());
            dto.setMessage("Appointment accepted by " + mechanic.getName());
            dto.setTitle("Appointment Expired");
            dto.setCreatedAt(Instant.now());

            simpMessagingTemplate.convertAndSend(
                    "/topic/appointment/expired/" + req.getMechanic().getId(),
                    dto
            );

            Map<String, String> fcmData = new HashMap<>();
            fcmData.put("type", NotificationType.APPOINTMENT_EXPIRED.toString());
            fcmData.put("appointmentId", req.getAppointment().getAppointmentId());
            fcmData.put("notificationId", String.valueOf(notification.getId()));

            fcmService.sendToMechanic(
                    req.getMechanic(),
                    "Appointment Expired",
                    "Appointment accepted by " + mechanic.getName(),
                    fcmData
            );
        }

        // ================= USER NOTIFICATION =================
        Notification notification = new Notification();
        notification.setType(NotificationType.APPOINTMENT_ACCEPTED);
        notification.setAppointments(appointment);
        notification.setTitle("Appointment Accepted");
        notification.setMessage("Appointment Accepted Successfully");
        notification.setMechanic(mechanic);
        notification.setUser(appointment.getUser());
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);

        AppointmentResponseDTO dto = new AppointmentResponseDTO();
        dto.setImage(appointment.getMechanic().getMechanicimgurl());
        dto.setMessage("Appointment Accepted Successfully");
        dto.setTitle("Appointment Accepted");
        dto.setCreatedAt(Instant.now());

        simpMessagingTemplate.convertAndSend(
                "/topic/appointment/acceptappointment/" + appointment.getUser().getUserid(),
                dto
        );

        Map<String, String> fcmData = new HashMap<>();
        fcmData.put("type", NotificationType.APPOINTMENT_ACCEPTED.toString());
        fcmData.put("appointmentId", appointment.getAppointmentId());
        fcmData.put("notificationId", String.valueOf(notification.getId()));

        fcmService.sendToUser(
                appointment.getUser(),
                "Appointment Accepted",
                "Appointment Accepted Successfully",
                fcmData
        );

        return ResponseEntity.ok("Appointment Accepted Successfully");
    }

    public ResponseEntity<?> startappointment(String phonenumber, String appointmentid) {
        Optional<Mechanic> checkmechanic = mechanicrepo.findByPhonenumber(phonenumber);
        if(checkmechanic.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic Not Found ");
        }
        Mechanic mechanic = checkmechanic.get();
        Optional<Appointments> checkappointment = appointmentRepository.findByMechanicAndAppointmentId(mechanic , appointmentid);
        Optional<AppointmentRequest> checkrequest = appointmentRequestRepository.findByMechanicAndAppointment_AppointmentId(mechanic , appointmentid);

        if  (checkappointment.isEmpty() ||  checkrequest.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Apki is Appointment say koy request nh hay");

        }
        Appointments appointments = checkappointment.get();
        AppointmentRequest appointmentRequest = checkrequest.get();

        if (appointments.getStatus() == AppointmentStatus.ON_THE_WAY) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Appointment already started");
        }

        if(appointments.getStatus()!= AppointmentStatus.ACCEPTED){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Action not allowed. Current status: " + appointments.getStatus());

        }

        appointments.setStatus(AppointmentStatus.ON_THE_WAY);
        appointmentRepository.save(appointments);

        appointmentRequest.setStatus(RequestStatus.ON_THE_WAY);
        appointmentRequest.setRespondedAt(Instant.now());
        appointmentRequestRepository.save(appointmentRequest);

        Notification notification = new Notification();
        notification.setType(NotificationType.MECHANIC_ON_THE_WAY);
        notification.setAppointments(appointments);
        notification.setUser(appointments.getUser());
        notification.setMechanic(mechanic);
        notification.setTitle("Mechanic On The Way");
        notification.setMessage("Your mechanic is on the way to your location");
        notification.setCreatedAt(Instant.now());

        notificationRepository.save(notification);

        AppointmentResponseDTO dto = new AppointmentResponseDTO();
        dto.setTitle("Mechanic On The Way");
        dto.setMessage("Your mechanic is on the way");
        dto.setImage(mechanic.getMechanicimgurl());
        dto.setCreatedAt(Instant.now());

        simpMessagingTemplate.convertAndSend("/topic/appointment/on-the-way/" + appointments.getUser().getUserid(), dto);
        Map<String, String> fcmData = new HashMap<>();
        fcmData.put("type",NotificationType.MECHANIC_ON_THE_WAY.toString());
        fcmData.put("appointmentId", appointments.getAppointmentId());
        fcmData.put("notificationId", String.valueOf(notification.getId()));

        fcmService.sendToUser(
                appointments.getUser(),
                "Mechanic On The Way",
                "Your mechanic is on the way to your location",
                fcmData
        );
  return ResponseEntity.ok("Appointment Started Successfully");
    }

    public ResponseEntity<?> arriveappointment(String phonenumber, String appointmentid) {

        Optional<Mechanic> checkmechanic = mechanicrepo.findByPhonenumber(phonenumber);
        if (checkmechanic.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic Not Found");
        }

        Mechanic mechanic = checkmechanic.get();

        Optional<Appointments> checkappointment =
                appointmentRepository.findByMechanicAndAppointmentId(mechanic, appointmentid);
        Optional<AppointmentRequest> checkrequest = appointmentRequestRepository.findByMechanicAndAppointment_AppointmentId(mechanic, appointmentid);
        if(checkrequest.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No appointment found") ;
        }

        if (checkappointment.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No appointment found");
        }

        Appointments appointment = checkappointment.get();
        AppointmentRequest request = checkrequest.get();


        if (appointment.getStatus() == AppointmentStatus.ARRIVED || request.getStatus()==RequestStatus.ARRIVED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("You Are already arrived");
        }
        if (appointment.getStatus() != AppointmentStatus.ON_THE_WAY  || request.getStatus()!=RequestStatus.ON_THE_WAY) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("You can only arrive after ON_THE_WAY");
        }
            request.setStatus(RequestStatus.ARRIVED);
        appointmentRequestRepository.save(request);

        appointment.setStatus(AppointmentStatus.ARRIVED);
        appointmentRepository.save(appointment);



        Notification notification = new Notification();
        notification.setType(NotificationType.MECHANIC_ARRIVED);
        notification.setAppointments(appointment);
        notification.setUser(appointment.getUser());
        notification.setMechanic(mechanic);
        notification.setTitle("Mechanic Arrived");
        notification.setMessage("Your mechanic has arrived at location");
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);

        AppointmentResponseDTO dto = new AppointmentResponseDTO();
        dto.setTitle("Mechanic Arrived");
        dto.setMessage("Mechanic has arrived");
        dto.setCreatedAt(Instant.now());
        dto.setImage(mechanic.getMechanicimgurl());

        simpMessagingTemplate.convertAndSend(
                "/topic/appointment/arrived/" + appointment.getUser().getUserid(),
                dto
        );

        return ResponseEntity.ok("Mechanic Arrived Successfully");
    }
    public ResponseEntity<?> startwork(String phonenumber, String appointmentid) {

        Optional<Mechanic> checkmechanic = mechanicrepo.findByPhonenumber(phonenumber);
        if (checkmechanic.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic Not Found");
        }

        Mechanic mechanic = checkmechanic.get();
        Optional<AppointmentRequest> checkrequest = appointmentRequestRepository.findByMechanicAndAppointment_AppointmentId(mechanic, appointmentid);

        Optional<Appointments> checkappointment =
                appointmentRepository.findByMechanicAndAppointmentId(mechanic, appointmentid);
        if(checkrequest.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No appointment found") ;
        }
        if (checkappointment.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No appointment found");
        }

        Appointments appointment = checkappointment.get();
        AppointmentRequest request = checkrequest.get();

        if (appointment.getStatus() == AppointmentStatus.IN_PROGRESS || request.getStatus()==RequestStatus.IN_PROGRESS) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("YOU Are ALready work started");
        }

        if (appointment.getStatus() != AppointmentStatus.ARRIVED  || request.getStatus()!=RequestStatus.ARRIVED ) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Work can only start after ARRIVED");
        }

        request.setStatus(RequestStatus.IN_PROGRESS);
        appointmentRequestRepository.save(request);

        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        appointmentRepository.save(appointment);

        Notification notification = new Notification();
        notification.setType(NotificationType.MECHANIC_WORK_STARTED);
        notification.setAppointments(appointment);
        notification.setUser(appointment.getUser());
        notification.setMechanic(mechanic);
        notification.setTitle("Work Started");
        notification.setMessage("Mechanic has started work");
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);

        AppointmentResponseDTO dto = new AppointmentResponseDTO();
        dto.setTitle("Work Started");
        dto.setMessage("Mechanic started work");
        dto.setCreatedAt(Instant.now());
        dto.setImage(mechanic.getMechanicimgurl());

        simpMessagingTemplate.convertAndSend(
                "/topic/appointment/in-progress/" + appointment.getUser().getUserid(),
                dto
        );

        return ResponseEntity.ok("Work Started Successfully");
    }


    public ResponseEntity<?> completework(String phonenumber, String appointmentid) {

        Optional<Mechanic> checkmechanic = mechanicrepo.findByPhonenumber(phonenumber);
        if (checkmechanic.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic Not Found");
        }

        Mechanic mechanic = checkmechanic.get();
        Optional<AppointmentRequest> checkrequest = appointmentRequestRepository.findByMechanicAndAppointment_AppointmentId(mechanic, appointmentid);

        Optional<Appointments> checkappointment =
                appointmentRepository.findByMechanicAndAppointmentId(mechanic, appointmentid);
        if(checkrequest.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No appointment found") ;
        }
        if (checkappointment.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No appointment found");
        }
        AppointmentRequest request = checkrequest.get();
        Appointments appointment = checkappointment.get();

        if (appointment.getStatus() == AppointmentStatus.WORK_COMPLETED || request.getStatus()==RequestStatus.WORK_COMPLETED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("You already completed this appointment");
        }

        if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS  || request.getStatus()!=RequestStatus.IN_PROGRESS ) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Work can only be completed after IN_PROGRESS");
        }

        appointment.setStatus(AppointmentStatus.WORK_COMPLETED);
        appointmentRepository.save(appointment);


        request.setStatus(RequestStatus.WORK_COMPLETED);
        appointmentRequestRepository.save(request);

        // 🔥 Payment trigger point
        Notification notification = new Notification();
        notification.setType(NotificationType.WORK_COMPLETED);
        notification.setAppointments(appointment);
        notification.setUser(appointment.getUser());
        notification.setMechanic(mechanic);
        notification.setTitle("Work Completed");
        notification.setMessage("Service completed, proceed to payment");
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);

        AppointmentResponseDTO dto = new AppointmentResponseDTO();
        dto.setTitle("Work Completed");
        dto.setMessage("Please proceed to payment");
        dto.setCreatedAt(Instant.now());
        dto.setImage(mechanic.getMechanicimgurl());

        simpMessagingTemplate.convertAndSend(
                "/topic/appointment/completework/" + appointment.getUser().getUserid(),
                dto
        );

        return ResponseEntity.ok("Work Completed Successfully");
    }

    public ResponseEntity<?> sendcharges (String phonenumber , String appointmentid, AppointmentPriceDTO dto){

        Optional<Mechanic> checkmechanic = mechanicrepo.findByPhonenumber(phonenumber);
        if (checkmechanic.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic Not Found");
        }

        Mechanic mechanic = checkmechanic.get();
        Optional<AppointmentRequest> checkrequest = appointmentRequestRepository.findByMechanicAndAppointment_AppointmentId(mechanic, appointmentid);

        Optional<Appointments> checkappointment =
                appointmentRepository.findByMechanicAndAppointmentId(mechanic, appointmentid);
        if(checkrequest.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No appointment found") ;
        }
        if (checkappointment.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No appointment found");
        }
        AppointmentRequest request = checkrequest.get();
        Appointments appointment = checkappointment.get();

        if (appointment.getStatus() != AppointmentStatus.WORK_COMPLETED  || request.getStatus()!=RequestStatus.WORK_COMPLETED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("First Complete your work");
        }

                    int visitingcharges =appointment.getVisitingCharge() ;
                System.out.println("this is visiting charges "+ visitingcharges);

                int  finalprice  =  dto.getFinalPrice() == null ? 0 : (int)dto.getFinalPrice().doubleValue();
                   appointment.setRepairAmount(finalprice);
                    appointment.setAmount(BigDecimal.valueOf(visitingcharges + finalprice));
                    appointment.setPaymentStatus(PaymentStatus.PENDING);
                    appointment.setStatus(AppointmentStatus.PAYMENT_PROCESS);
                    appointmentRepository.save(appointment);

        ;
                    request.setStatus(RequestStatus.PAYMENT_PROCESS) ;
                    appointmentRequestRepository.save(request);

                 AppointmentResponseDTO dto1 = new AppointmentResponseDTO();
                        dto1.setTitle("Send Charges");
                        dto1.setMessage("Please proceed to payment");
                        dto1.setCreatedAt(Instant.now());
                        dto1.setImage(mechanic.getMechanicimgurl());
                 simpMessagingTemplate.convertAndSend(
                "/topic/appointment/sendcharges/" + appointment.getUser().getUserid(),
                dto1
        );

        return ResponseEntity.ok("Send charges successfully");



    }


    public ResponseEntity<?> paycash(String phonenumber, String appointmentid) {
        Optional<Mechanic> checkmechanic = mechanicrepo.findByPhonenumber(phonenumber);
        if (checkmechanic.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic Not Found");
        }

        Mechanic mechanic = checkmechanic.get();

        Optional<Appointments> checkappointment =
                appointmentRepository.findByMechanicAndAppointmentId(mechanic, appointmentid);

        if (checkappointment.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No appointment found");
        }

        Appointments appointment = checkappointment.get();

        if (appointment.getStatus() != AppointmentStatus.PAYMENT_PROCESS) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("First Complete your work");
        }
        appointment.setPaymentStatus(PaymentStatus.PAID);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setCompletedAt(Instant.now());
        appointment.setPaymentMethod("CASH");
        appointmentRepository.save(appointment);


        mechanic.setIsengaged(false);
        mechanic.setTotalJobsCompleted(mechanic.getTotalJobsCompleted() +1 );
        mechanic.setTotalearning(mechanic.getTotalearning() + appointment.getAmount().intValue());

        mechanicrepo.save(mechanic);


        Notification notification = new Notification();
        notification.setType(NotificationType.APPOINTMENT_COMPLETED);
        notification.setAppointments(appointment);
        notification.setUser(appointment.getUser());
        notification.setMechanic(mechanic);
        notification.setTitle("Payment Successfully");
        notification.setMessage("Payment successfully done");
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);

        AppointmentResponseDTO dto1 = new AppointmentResponseDTO();
        dto1.setTitle("Payment Done");
        dto1.setMessage("Appointment Completed successfully");
        dto1.setCreatedAt(Instant.now());
        dto1.setImage(mechanic.getMechanicimgurl());
        simpMessagingTemplate.convertAndSend(
                "/topic/appointment/appointmentdone/" + appointment.getMechanic().getId(),
                dto1 ) ;

        return  ResponseEntity.ok("Payment Successfully and job done");

 }
}

