package com.haris.MechanicApp.Service;

import com.haris.MechanicApp.Model.Appointments.*;
import com.haris.MechanicApp.Model.GoogleDistance;
import com.haris.MechanicApp.Model.Location.Location;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Mechanic.MechanicDTO;
import com.haris.MechanicApp.Model.Notification.MechanicNotification;
import com.haris.MechanicApp.Model.Notification.NotificationType;
import com.haris.MechanicApp.Model.RoadInfo;
import com.haris.MechanicApp.Model.Verification.User;
import com.haris.MechanicApp.Repository.AppointmentRepository;
import com.haris.MechanicApp.Repository.MechanicNotificationRepository;
import com.haris.MechanicApp.Repository.MechanicRepository;
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

                MechanicNotification notification =  new MechanicNotification();
                notification.setAppointments(appointments);
                notification.setMechanic(mechanic);
                notification.setMessage("You received a new appointment request");
                notification.setTitle("New Booking");
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
        Optional<Mechanic> checkmechanic = mechanicrepo.findById(appointmentDto.getId());
        Optional<User> checkuser = userRepo.findByPhonenumber(userphonenumber);



        if(checkuser.isPresent() &&  checkmechanic.isPresent()) {
            System.out.println("User hayga yrr phir"+ checkuser.get().getPhonenumber());
            User user = checkuser.get();
            Mechanic mechanic = checkmechanic.get();
            Appointments appointments = new Appointments();
            appointments.setMechanic(mechanic);
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
            appointmentRepository.save(appointments);

            MechanicNotification notification =  new MechanicNotification();
            notification.setAppointments(appointments);
            notification.setMechanic(mechanic);
            notification.setMessage("You received a new appointment request");
            notification.setTitle("New Booking");
            notification.setType(NotificationType.APPOINTMENT_REQUEST);
            notification.setCreatedAt(Instant.now() );
            notificationRepository.save(notification);

            BookingNotificationDto dto =  new BookingNotificationDto() ;
            dto.setAppointmentid(appointments.getAppointmentId());
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
            dto.setMechshoplong(mechanic.getShoplongitude());
            dto.setCreated_at(notification.getCreatedAt());

            dto.setMechname(mechanic.getName());
            dto.setMechexperience(mechanic.getExperienceyears());
            dto.setMechimage(mechanic.getMechanicimgurl());
            dto.setMechtype(mechanic.getMechanictype());
            dto.setTotalreviews(mechanic.getTotalReviews());
            dto.setMechrating(mechanic.getAverageRating());
            dto.setNotificationid(notification.getId());

            long mechanicid = mechanic.getId();
            String destination = "/topic/bookappointment/nearbymechanics/" + mechanicid;
            simpMessagingTemplate.convertAndSend(destination, dto);
            return ResponseEntity.ok(dto);

        }
        return   ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found");
    }

    public ResponseEntity<?> mechanicallnotifications(String phonenumber) {
        Optional<Mechanic> checkmechanic = mechanicrepo.findByPhonenumber(phonenumber);
        if(checkmechanic.isPresent()) {
            Mechanic mechanic =  checkmechanic.get();
            List<BookingNotificationDto> bookingNotificationDtos = new   ArrayList<>();
               List<MechanicNotification> allnotifications =  notificationRepository.findByMechanicIdOrderByCreatedAtDesc(mechanic.getId());
               for (MechanicNotification notification : allnotifications) {
                   BookingNotificationDto dto =  new BookingNotificationDto();
                    dto.setNotificationid(notification.getId());
                    dto.setAddress(notification.getAppointments().getAddress());
                    dto.setLatitude(notification.getAppointments().getLatitude());
                    dto.setLongitude(notification.getAppointments().getLongitude());
                    dto.setServiceType(notification.getAppointments().getServiceType());
                    dto.setAppointmentTime(notification.getAppointments().getAppointmentTime());
                    dto.setAppointmentDate(notification.getAppointments().getAppointmentDate());
                    dto.setProblemDescription(notification.getAppointments().getProblemDescription());
                    dto.setUserphonenumber(notification.getAppointments().getUser().getPhonenumber());
                    dto.setUserimage(notification.getAppointments().getUser().getUserimgurl());
                    dto.setUsername(notification.getAppointments().getUser().getUsername());
                    dto.setMechshoplat(notification.getMechanic().getShoplatitude());
                    dto.setMechshoplong(notification.getMechanic().getShoplongitude());
                    dto.setCreated_at(notification.getCreatedAt());
                    dto.setIsread(notification.isRead());
                    bookingNotificationDtos.add(dto);

               }

                    return  ResponseEntity.ok( bookingNotificationDtos);
        }
        return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic not found");
    }

   public void isreadnotification(String userphonenumber, long notificationid) {
        Optional<Mechanic> ismechanic = mechanicrepo.findByPhonenumber(userphonenumber);

        if(ismechanic.isPresent()) {
            Mechanic mechanic = ismechanic.get();
            Optional<MechanicNotification> notification = notificationRepository.findByIdAndMechanic(notificationid,mechanic);
            if(notification.isPresent()) {
                MechanicNotification mechanicnotification = notification.get();
                mechanicnotification.setRead(true);
                notificationRepository.save(mechanicnotification);
                System.out.println("may read hogya hn");

            }

        }

    }
}
