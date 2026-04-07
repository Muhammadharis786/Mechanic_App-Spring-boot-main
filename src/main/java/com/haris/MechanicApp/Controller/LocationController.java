package com.haris.MechanicApp.Controller;


import com.haris.MechanicApp.Model.Location.Location;
import com.haris.MechanicApp.Model.Verification.User;
import com.haris.MechanicApp.Service.LocationService;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LocationController {

    @Autowired
    LocationService locationService;

    @PostMapping("api/current/location")
    public ResponseEntity<?> userCurrentLocation(@RequestBody Location location ,
                                                 @AuthenticationPrincipal UserDetails userDetails ){
            String phonenumber = userDetails.getUsername();
        return locationService.userCurrentLocation(location , phonenumber);
 }

 @PostMapping ("api/mechanic/currentlocation")
    public void addMechLocation (@RequestBody  Location location,
                                              @AuthenticationPrincipal UserDetails userDetails ){
        String phonenumber = userDetails.getUsername();
         locationService.addMechanicLocations(location , phonenumber);
 }


}
