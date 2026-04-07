package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Model.User.UserDto;
import com.haris.MechanicApp.Service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
public class NotificationController {
@Autowired
    NotificationService notificationService;

    @GetMapping("api/service/request/mechanic/{mechphonenumber}")
    public ResponseEntity<?> mechanicRequest( @PathVariable String mechphonenumber ,
                                                @AuthenticationPrincipal UserDetails userDetails) {
         String phonenumber =  userDetails.getUsername();
    return       notificationService.requestonemechanic( phonenumber , mechphonenumber);


    }

    @GetMapping ("api/service/request/mechanic")
    public ResponseEntity<?> mechanicallRequest(
                                              @AuthenticationPrincipal UserDetails userDetails) {
        String phonenumber =  userDetails.getUsername();
        return       notificationService.requestallmechanic( phonenumber);


    }

    @GetMapping ("api/service/request/nearbymechanic")
    public ResponseEntity<?> nearbymechanicRequest(
            @AuthenticationPrincipal UserDetails userDetails) {
        String phonenumber =  userDetails.getUsername();
        return       notificationService.nearbymechanic( phonenumber);


    }
}
