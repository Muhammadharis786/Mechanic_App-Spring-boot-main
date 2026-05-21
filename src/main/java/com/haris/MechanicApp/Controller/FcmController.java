package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Model.FCM.FcmTokenRequestDTO;
import com.haris.MechanicApp.Service.FcmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

@RestController
@RequestMapping("/api/fcm")
public class FcmController {

    @Autowired
    private FcmService fcmService;
// ye tb call hogi jab user or mechanic login kraiga aur us time per mujhay us mobile ka token daiga
      // aur sath hi sath may btaiga
         // user  or mechanci

    @PostMapping("/token")
    public ResponseEntity<String> saveToken(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody FcmTokenRequestDTO request
    ) {
        String base64 = authHeader.replace("Basic ", "");

        // 2. Decode Base64
        String decoded = new String(Base64.getDecoder().decode(base64));

// 3. Split phone;role:password
        String[] firstSplit = decoded.split(";");

        String phone = firstSplit[0];

        String[] roleAndPass = firstSplit[1].split(":");

        String role = roleAndPass[0];
         fcmService.saveToken(phone , role , request);
        return ResponseEntity.ok("FCM token saved");



    }
}