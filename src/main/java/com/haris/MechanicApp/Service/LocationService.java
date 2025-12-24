package com.haris.MechanicApp.Service;


import com.haris.MechanicApp.Model.Location.Location;
import com.haris.MechanicApp.Model.Verification.User;
import com.haris.MechanicApp.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class LocationService {

    @Autowired
    UserRepository userRepo;



    // ye logined user ki location laikerdatabase may save kraiga
    public ResponseEntity<?> userCurrentLocation(Location location, String email) {
        User getUser = userRepo.findByEmail(email).get();
        getUser.setLastLatitude(location.getLatitude());
        getUser.setLastLongitude(location.getLongitude());
          return  ResponseEntity.ok(userRepo.save(getUser));


    }
}