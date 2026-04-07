package com.haris.MechanicApp.Service;

import com.haris.MechanicApp.Model.GoogleDistance;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Request.RequestUserDto;
import com.haris.MechanicApp.Model.User.UserDto;
import com.haris.MechanicApp.Model.Verification.User;
import com.haris.MechanicApp.Repository.MechanicRepository;
import com.haris.MechanicApp.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    UserRepository userRepository;

    @Autowired
    private RedisTemplate<String , String > redisTemplate;

    MechanicRepository  mechanicRepository;
    public ResponseEntity<?> requestonemechanic(String phonenumber, String mechphonenumber) {

        Optional<User> checkuser =  userRepository.findByPhonenumber(phonenumber);
        if(checkuser.isPresent()){
            User user = checkuser.get();
            UserDto   userDto = new UserDto();
             userDto.setUsername(user.getUsername());
             userDto.setUserid(user.getUserid());
                   String destination = "/topic/mechanic/" +  mechphonenumber;
            System.out.println(destination);
           simpMessagingTemplate.convertAndSend(destination, userDto);
            return ResponseEntity.ok(userDto);
        }
        return ResponseEntity.notFound().build();



    }

    public ResponseEntity<?> requestallmechanic(String phonenumber) {
        Optional<User> checkuser =  userRepository.findByPhonenumber(phonenumber);
        if(checkuser.isPresent()){
            User user = checkuser.get();
            UserDto   userDto = new UserDto();
            userDto.setUsername(user.getUsername());
            userDto.setUserid(user.getUserid());
            String destination = "/topic/all-mechanics" ;
            System.out.println(destination);
            simpMessagingTemplate.convertAndSend(destination, userDto);
            return ResponseEntity.ok(userDto);
        }
        return ResponseEntity.notFound().build();

    }

    public ResponseEntity<?> nearbymechanic(String phonenumber) {
        Optional<User> checkuser =  userRepository.findByPhonenumber(phonenumber);
         if(checkuser.isPresent()){
            User user = checkuser.get();
            GeoOperations<String  , String> geoOperations = redisTemplate.opsForGeo();
            double userlongitude =  user.getLastLongitude().doubleValue();
            double userlatitude  = user.getLastLatitude().doubleValue();
            //this is for search mechanic and calcute distance bw user and mechanic and give nearbymechanic
             //that define specific range and save in results type object


            GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                    geoOperations.search(
                            "mechanic",
                            GeoReference.fromCoordinate(userlongitude, userlatitude),
                            new Distance(65, Metrics.KILOMETERS),
                            RedisGeoCommands.GeoSearchCommandArgs
                                    .newGeoSearchArgs()
                                    .includeDistance()

                    );
            GoogleDistance UserLocationName =  new GoogleDistance();
            RequestUserDto reqDto = new RequestUserDto();
            String locname =     UserLocationName.getAddressFromLatLng(user.getLastLatitude() , user.getLastLongitude());
            reqDto.setUserlocname(locname);
            reqDto.setUserid(user.getUserid());
            reqDto.setPrice(1200);
            reqDto.setUsername(user.getUsername());

            for (GeoResult<RedisGeoCommands.GeoLocation<String>> result  : results){
                long mechanicid =  Long.parseLong (result.getContent().getName());
                double  distance = result.getDistance().getValue();

                System.out.println(distance );
                 reqDto.setDistance(distance);
                 String destination = "/topic/nearbymechanics/" + mechanicid;
                System.out.println(destination);
                simpMessagingTemplate.convertAndSend(destination, reqDto);

}
             return ResponseEntity.ok(reqDto);
 }
        return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("User Not Found");
    }
}
