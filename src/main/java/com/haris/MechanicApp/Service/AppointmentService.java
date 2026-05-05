package com.haris.MechanicApp.Service;

import com.haris.MechanicApp.Model.Appointments.AutoAppointmentDto;
import com.haris.MechanicApp.Model.Appointments.AppointmentStatus;
import com.haris.MechanicApp.Model.Appointments.Appointments;
import com.haris.MechanicApp.Model.Appointments.ManualAppointmentDto;
import com.haris.MechanicApp.Model.GoogleDistance;
import com.haris.MechanicApp.Model.Location.Location;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Mechanic.MechanicDTO;
import com.haris.MechanicApp.Model.RoadInfo;
import com.haris.MechanicApp.Model.Verification.User;
import com.haris.MechanicApp.Repository.AppointmentRepository;
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
            appointments.setCreatedAt(modernDate());
            appointments.setServiceType(appointmentDto.getServiceType());
            appointments.setLatitude(appointmentDto.getLatitude());
            appointments.setLongitude(appointmentDto.getLongitude());
            appointments.setAddress(appointmentDto.getAddress());
            appointments.setStatus(AppointmentStatus.PENDING);
            String mechanictyperequest = appointmentDto.getServiceType().toUpperCase();
             mechanictyperequest=  mechanictyperequest.split(" ")[0];
            System.out.println(mechanictyperequest);

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
            for (GeoResult<RedisGeoCommands.GeoLocation<String>> result  : results){
                long mechanicid =  Long.parseLong (result.getContent().getName());
                 String mechanictype =   (String) redisTemplate.opsForHash()
                            .get("mechanic:details:" + mechanicid, "serviceType");

                // ✅ Sirf wahi mechanic add karo jiska service type match kare
                if(mechanictyperequest.equalsIgnoreCase(mechanictype)){
                    mechanicIds.add(mechanicid);
                }
            }
            if(mechanicIds.isEmpty()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No mechanics available");
            }

            for (Long mechanicids : mechanicIds){
                String destination = "/topic/bookappointment/nearbymechanics/" + mechanicids;
                simpMessagingTemplate.convertAndSend(destination, appointments);

            }
            return ResponseEntity.status(200).body(appointments);



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
            if(destinationsparam.length()>0){
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



        if(checkuser.isPresent()) {
            System.out.println("User hayga yrr phir"+ checkuser.get().getPhonenumber());
            User user = checkuser.get();
            Mechanic mechanic = checkmechanic.get();
            Appointments appointments = new Appointments();
            appointments.setMechanic(mechanic);
            appointments.setUser(user);
            appointments.setAppointmentDate(appointmentDto.getAppointmentDate());
            appointments.setAppointmentTime(appointmentDto.getAppointmentTime());
            appointments.setProblemDescription(appointmentDto.getProblemDescription());
            appointments.setCreatedAt(modernDate());
            appointments.setServiceType(appointmentDto.getServiceType());
            appointments.setLatitude(appointmentDto.getLatitude());
            appointments.setLongitude(appointmentDto.getLongitude());
            appointments.setAddress(appointmentDto.getAddress());
            appointments.setStatus(AppointmentStatus.PENDING);
            appointmentRepository.save(appointments);
            long mechanicid = mechanic.getId();
            String destination = "/topic/bookappointment/nearbymechanics/" + mechanicid;
            simpMessagingTemplate.convertAndSend(destination, appointments);
            return ResponseEntity.ok(appointments);


 }
        return   ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found");
    }
}
