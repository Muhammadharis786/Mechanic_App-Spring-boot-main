package com.haris.MechanicApp.Service;


import com.haris.MechanicApp.Model.Location.Location;
import com.haris.MechanicApp.Model.Verification.User;
import com.haris.MechanicApp.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LocationService {

    @Autowired
    UserRepository userRepo;



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
}