package com.haris.MechanicApp.Service;


import com.haris.MechanicApp.Model.Location.Location;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Verification.User;
import com.haris.MechanicApp.Repository.MechanicRepository;
import com.haris.MechanicApp.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class LocationService {

    @Autowired
    UserRepository userRepo;

    @Autowired
    private RedisTemplate<String , String > redisTemplate;

    @Autowired
    private MechanicRepository  mechRepo;




    // ye logined user ki location laikerdatabase may save kraiga
    public ResponseEntity<?> userCurrentLocation(Location location, String phonenumber) {
         Optional<User>  getUser = userRepo.findByPhonenumber(phonenumber);
        if(getUser.isPresent()){
            User currentUser = getUser.get();
            currentUser.setLastLatitude(location.getLatitude());
            currentUser.setLastLongitude(location.getLongitude());
            return  ResponseEntity.ok(userRepo.save(currentUser));
        }
        return
                ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");



    }
    public void  addMechanicLocations(Location location , String mechphonenumber) {
    Optional<Mechanic> ismechanic =   mechRepo.findByPhonenumber(mechphonenumber);
    if(ismechanic.isPresent()){
        Mechanic currentMechanic = ismechanic.get();
        currentMechanic.setLongitude(location.getLongitude());
        currentMechanic.setLatitude(location.getLatitude());
        mechRepo.save(currentMechanic);
        String mechid = currentMechanic.getId().toString();
        double latitude  =   (location.getLatitude()).doubleValue()  ;
        double longitude = location.getLongitude().doubleValue();
        GeoOperations <String  , String> geoOperations = redisTemplate.opsForGeo();
        geoOperations.add("mechanic",
                    new Point( longitude , latitude ),
                    mechid ) ;
        System.out.println("Mehcnaic Locations added: "+ longitude +" "+ latitude);
}
        System.out.println("Mechanic not found");

     }

    public void  addcUserLocations(Location location , String usernumber) {
        Optional<User> isuser =   userRepo.findByPhonenumber(usernumber);
        if(isuser.isPresent()){
            User user = isuser.get();


            user.setLastLatitude(location.getLatitude());
            user.setLastLongitude(location.getLongitude());
            userRepo.save(user);

            String userid = String.valueOf(user.getUserid());
            double latitude  =   (location.getLatitude()).doubleValue()  ;
            double longitude = location.getLongitude().doubleValue();
            GeoOperations <String  , String> geoOperations = redisTemplate.opsForGeo();
            geoOperations.add("user",
                    new Point( longitude , latitude ),
                    userid ) ;
            System.out.println("User Locations added: "+ longitude +" "+ latitude);
        }
        System.out.println("User not found");
    }
}