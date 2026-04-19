package com.haris.MechanicApp.Service;

import com.haris.MechanicApp.Model.GoogleDistance;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Request.RequestUserDto;
import com.haris.MechanicApp.Model.RoadInfo;
import com.haris.MechanicApp.Model.User.UserDto;
import com.haris.MechanicApp.Model.Verification.User;
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
            double userlongitude ;
            double userlatitude  ;
            List<Point> usercordinates = geoOperations.position("user", String.valueOf(user.getUserid()));
            //this is for search mechanic and calcute distance bw user and mechanic and give nearbymechanic
             //that define specific range and save in results type object
             if  ( usercordinates != null && ! usercordinates.isEmpty() &&  usercordinates.get(0) != null) {
                 userlongitude =  usercordinates.get(0).getX();
                 userlatitude =  usercordinates.get(0).getY();
                 System.out.println("Fetched User position from REDIS: " + userlatitude + ", " + userlongitude);
             } else {
                 // Fallback if not in Redis
                 userlongitude = user.getLastLongitude().doubleValue();
                 userlatitude = user.getLastLatitude().doubleValue();
             }

// 1. Redis search with Coordinates
            GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                    geoOperations.search(
                            "mechanic",
                            GeoReference.fromCoordinate(userlongitude, userlatitude),
                            new Distance(65, Metrics.KILOMETERS),
                            RedisGeoCommands.GeoSearchCommandArgs
                                    .newGeoSearchArgs()
                                    .includeDistance()
                                    .includeCoordinates()

                    );


             StringBuilder destinationsparam = new StringBuilder();
             List<Long> mechanicIds =  new ArrayList<>();
// 2. Sirf data jama karein loop mein
            for (GeoResult<RedisGeoCommands.GeoLocation<String>> result  : results){
                long mechanicid =  Long.parseLong (result.getContent().getName());
              Point   point = result.getContent().getPoint();
                destinationsparam.append(point.getY()).append(",").append(point.getX()).append("|");
                mechanicIds.add(mechanicid);



}// last wla pipe nikalnay ki lie

            if(destinationsparam.length()>0){
                destinationsparam.setLength(destinationsparam.length()-1);
            }
             System.out.println(destinationsparam);
             System.out.println("User latitude: "+ userlatitude);
             System.out.println("User longitude: "+ userlongitude);
// 3. API HIT karein (Ek hi baar)
// Aapka Google distance service ek json return karega jismein utne hi results honge jitne mechanics bhejay.
             GoogleDistance googleapi = new GoogleDistance();
             String locname =     googleapi.getAddressFromLatLng(
                    userlatitude , userlongitude);

            //wha say mujhay distance mila aur menay roadistances wkay list may object assign krdya
             List<RoadInfo> roadDistances = googleapi.getBatchRoadDistances(
                     userlatitude, userlongitude, destinationsparam.toString()
             );
Map<String , Double> distances = new HashMap<>();
             // 4. Ab naye loop mein actual road distance set karke WebSocket pe bhejein!
             for (int i = 0; i < mechanicIds.size(); i++) {
                 long mechId = mechanicIds.get(i);
                 RoadInfo info = roadDistances.get(i); // Road distance from Google Matrix
                 // Naya object banayein taa ke concurrency ka issue na ho
                 RequestUserDto reqDto = new RequestUserDto();
                 reqDto.setUserlocname(locname);
                 reqDto.setUserid(user.getUserid());
                 reqDto.setPrice(1200);
                 reqDto.setEta(info.getDistancetime());
                 reqDto.setUsername(user.getUsername());
                 reqDto.setUserimage(user.getUserimgurl());
                 reqDto.setLat(userlatitude);
                 reqDto.setLon(userlongitude);
                 reqDto.setDistance(info.getDistance());
                 String destination = "/topic/nearbymechanics/" + mechId;
                 simpMessagingTemplate.convertAndSend(destination, reqDto);
                 distances.put(info.getDistancetime(), info.getDistance());

             }

            return ResponseEntity.ok(distances);
 }
        return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("User Not Found");
    }
}
