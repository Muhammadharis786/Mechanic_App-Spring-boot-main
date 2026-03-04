package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Model.User.UserDto;
import com.haris.MechanicApp.Model.Verification.DtoUser;
import com.haris.MechanicApp.Model.Verification.ForgotNumber;
import com.haris.MechanicApp.Model.Verification.Token;
import com.haris.MechanicApp.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class UserController {

    @Autowired
    AuthenticationManager authenticationManager;


    @Autowired
    UserService userService;


    @GetMapping("/")
    public String home() {

        return "hello world";
    }


    @GetMapping("api/user/allusers")
    public ResponseEntity<?> allusers() {

        return userService.allusers();
    }


    @DeleteMapping("api/user/delete/{userid}")
    public ResponseEntity<?> dltuser(@PathVariable long userid) {
        return userService.dltuser(userid);

    }

    @PostMapping("api/user/register")

    public ResponseEntity<?> registration(@RequestBody DtoUser user) {

        return userService.register(user);
    }


    @PostMapping("api/verify/user/token")
    public ResponseEntity<?> verifyRegistration(@RequestBody Token token
    ) {
        return userService.verifyRegistration(token.getToken(), token.getPhonenumber());
    }

    @PostMapping("api/login")
    public ResponseEntity<?> loginUser(@RequestBody DtoUser user) {
        return userService.login(user, authenticationManager);
    }

    @PostMapping("api/user/forgot")
    public ResponseEntity<?> forgotPasswords(@RequestBody ForgotNumber number) {

        return userService.forgotpasswords(number.getPhonenumber());

    }

    @PostMapping("api/user/forget/verify")
    public ResponseEntity<?> verifynewPasswordToken(@RequestBody Token token) {

        return userService.verifynewPasswordToken(token);

    }

    @PostMapping("api/user/newPassword")
    public ResponseEntity<?> updatepassword(@RequestBody DtoUser user) {

        return userService.updatePassword(user);

    }

    //ye simple user ki image save krnay k lie optional hay ager day tu thk wrna n
    @PutMapping(value = "api/save/user/userimage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)

    public ResponseEntity<?> updateUser(
            @RequestPart("userimage") MultipartFile userimage,
            @RequestPart("userdata") UserDto userDto,
            @AuthenticationPrincipal UserDetails userDetails) {

        String phonenumber = userDetails.getUsername();
        return userService.updateUser(userDto, userimage, phonenumber);

    }

    @GetMapping("api/user/dashboard")

    public ResponseEntity<?> userdashboard(
            @AuthenticationPrincipal UserDetails   userDetails) {
        System.out.println("user dashboard controller");
        String identifier = userDetails.getUsername();
      return userService.dashboard (identifier);
    }

}