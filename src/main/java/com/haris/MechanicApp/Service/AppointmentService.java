package com.haris.MechanicApp.Service;

import com.google.api.Http;
import com.haris.MechanicApp.Model.Appointments.*;
import com.haris.MechanicApp.Model.GoogleDistance;
import com.haris.MechanicApp.Model.Location.Location;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Mechanic.MechanicDTO;
import com.haris.MechanicApp.Model.Notification.MechanicNotificationDto;
import com.haris.MechanicApp.Model.Notification.Notification;
import com.haris.MechanicApp.Model.Notification.NotificationType;
import com.haris.MechanicApp.Model.Notification.UserNotificationDto;
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
                dto.setVisitingcharges(appointments.getVisitingCharge()); // Visiting charge hamesha dikhayein
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
        if(checkmechanic.isPresent()) {
            Mechanic mechanic = checkmechanic.get();
            List<AppointmentRequest> mechappointments = appointmentRequestRepository.findByMechanic(mechanic);
            if(mechappointments.isEmpty()) {
                return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("No Appointments ");
            }
            List<MechanicAppointmentDTO>listofappointments =  new ArrayList<>();
            for(AppointmentRequest appointments : mechappointments) {

                MechanicAppointmentDTO dto = new MechanicAppointmentDTO();

                //this is user address wo address hay jha per isko service chie
                dto.setAppointmentid(appointments.getAppointment().getAppointmentId());
                dto.setStatus(appointments.getStatus());
                dto.setAppointmentDate(appointments.getAppointment().getAppointmentDate());
                dto.setAppointmentTime(appointments.getAppointment().getAppointmentTime());
                dto.setProblemDescription(appointments.getAppointment().getProblemDescription());
                dto.setLatitude(appointments.getAppointment().getLatitude());
                dto.setLongitude(appointments.getAppointment().getLongitude());
                dto.setServiceType(appointments.getAppointment().getServiceType());
                dto.setCreated_at(appointments.getCreatedAt());
                dto.setReason(appointments.getReason());
                dto.setVisitingcharges(appointments.getAppointment().getVisitingCharge());
                dto.setRespondedat(appointments.getRespondedAt());

                dto.setUseraddress(appointments.getAppointment().getAddress());
                dto.setUsername(appointments.getAppointment().getUser().getUsername());
                dto.setUserimage(appointments.getAppointment().getUser().getUserimgurl());
                dto.setMechshoplat(appointments.getMechanic().getShoplatitude());
                dto.setMechshoplong(appointments.getMechanic().getShoplongitude());
                dto.setUserphonenumber(appointments.getAppointment().getUser().getPhonenumber());
                //ye uskay lie ha just reject or cancelled k lie

                listofappointments.add(dto);

            }
            return  ResponseEntity.ok(listofappointments);


        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic nh mila");
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
        }

        // =====================================
        // FINAL RESPONSE
        // =====================================
        return ResponseEntity.ok("Appointment Cancelled Successfully");
    }

    public ResponseEntity<?> acceptappointment(String phonenumber, String appointmentid) {
        Optional<Mechanic> checkmechanic = mechanicrepo.findByPhonenumber(phonenumber);
        Optional<Appointments> checkappointments = appointmentRepository.findByAppointmentId(appointmentid);
        if(checkmechanic.isPresent() && checkappointments.isPresent()) {
            Mechanic mechanic = checkmechanic.get();
            Appointments appointment = checkappointments.get();
            Optional<AppointmentRequest> checkrequest = appointmentRequestRepository.findByMechanicAndAppointment(mechanic , appointment);
            if (checkrequest.isPresent()){
                AppointmentRequest  request = checkrequest.get();
                if(request.getStatus()==RequestStatus.PENDING){

                    //this is for appiontment request
                    request.setStatus(RequestStatus.ACCEPTED);
                    request.setRespondedAt(Instant.now());
                    request.setAcceptedByMechanicId(mechanic.getId()); // 🔥 IMPORTANT FIELD
                    appointmentRequestRepository.save(request);




                    //this is for actual appointment
                    appointment.setStatus(AppointmentStatus.ACCEPTED);
                    appointment.setMechanic(mechanic);
                    appointmentRepository.save(appointment);

                    List<AppointmentRequest> allRequests =
                            appointmentRequestRepository.findByAppointment(appointment);

                    if(allRequests.isEmpty()){
                        return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("Appointment Not Found");
                    }

                    for (AppointmentRequest req : allRequests) {

                        if (!req.getMechanic().getId().equals(mechanic.getId())) {

                            req.setStatus(RequestStatus.EXPIRED);
                            req.setReason("Appointment has been accepted by " + mechanic.getName());
                            req.setRespondedAt(Instant.now());
                            req.setAcceptedByMechanicId(mechanic.getId()); // optional tracking
                            appointmentRequestRepository.save(req);

                        }
                    }


                    Notification notification = new Notification();
                    notification.setType(NotificationType.APPOINTMENT_ACCEPTED);
                    notification.setAppointments(appointment);
                    notification.setTitle("Appointment Accepted");
                    notification.setMessage("Appointment Accepted Successfully");
                    notification.setMechanic(mechanic);
                    notification.setUser(appointment.getUser());
                    notification.setCreatedAt(Instant.now());
                    notificationRepository.save(notification);

                    AppointmentResponseDTO responseDTO = new AppointmentResponseDTO();
                    responseDTO.setImage(appointment.getMechanic().getMechanicimgurl());
                    responseDTO.setMessage("Appointment Accepted Successfully");
                    responseDTO.setTitle("Appointment Accepted");
                    responseDTO.setCreatedAt(Instant.now());
                    long userid = appointment.getUser().getUserid();
                    String destination = "/topic/appointment/acceptappointment/" + userid;
                    simpMessagingTemplate.convertAndSend(destination , responseDTO);
                    return ResponseEntity.ok("Appointment Accepted Successfully");

                }
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Apka appointment ka status pending nh hay");

            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Apki pass is"+ appointmentid + " say koy request nh hay");
        }


    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic Not Found or Appointment not found");
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
  return ResponseEntity.ok("Appointment Started Successfully");
    }



}

