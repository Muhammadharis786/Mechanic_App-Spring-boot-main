package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Model.Mechanic.*;
import com.haris.MechanicApp.Model.User.UserDto;
import com.haris.MechanicApp.Model.Verification.DtoUser;
import com.haris.MechanicApp.Model.Verification.ForgotNumber;
import com.haris.MechanicApp.Model.Verification.Token;
import com.haris.MechanicApp.Repository.MechanicRepository;
import com.haris.MechanicApp.Service.MechanicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class MechanicController {

    @Autowired
    private MechanicRepository mechanicRepo;
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    private MechanicService mechanicService;
    @GetMapping ("api/mechanic/allmechanic")
    public List<Mechanic> getAllMechanic(){



        return mechanicRepo.findAll();
 }




    @DeleteMapping ("api/delete/mechanic/{mechid}")

 public ResponseEntity<?> deletemechanic (@PathVariable long mechid ){

        return mechanicService.dltmechani(mechid);


 }
 //ye check kraiga kay mechanic online hay offline
    @PostMapping("api/mechanic/checkinginternet/{id}/heartbeat") public void heartbeat(@PathVariable Long id) {


        mechanicService.updateLastSeen(id); }

    @PostMapping ("api/mechanic/jobstatus")
    public ResponseEntity<?> updateEngaged(
            @PathVariable Long id,
            @RequestParam boolean engaged) {

      mechanicService.updateEngagedStatus(id, engaged);
        return ResponseEntity.ok("Mechanic status updated");
    }

    //fist mechanic send otp on whatsapp number
    @PostMapping ("api/mechanic/registerwithotp")
    public ResponseEntity<?> registerwithotp( @RequestBody MechOtpDto otpDto ){

       return    mechanicService.mechanicOtp( otpDto);

    }
    //second mechanic verify otp with existing otp that save in database if match so move to reigster api
    @PostMapping ("api/mechanic/register/verify")

    public ResponseEntity <?> verifymechanicregistration(@RequestBody Token token){

        return  mechanicService.verifymechanic (token);
    }

    //last one to fill the form of mechanic fisrtly check userid and phonumber to get existing mechanic that
    //verify itself with phone number

    @PostMapping(value = "api/mechanic/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerMechanic(
            @RequestPart("userData") MechanicRegistrationDto mechanicdata,
            @RequestPart("mechanicprofilePicture") MultipartFile mecanicimg,
            @RequestPart("cnicfrontimg") MultipartFile cnicfrontimg,
            @RequestPart("cnicbackimg") MultipartFile cnicbackimg

            ){
        return  mechanicService.registerMechanic (mechanicdata , mecanicimg , cnicbackimg ,cnicfrontimg);


    }

        @PostMapping("api/mechanic/forget")
    public ResponseEntity<?> forgotmechanic(
      @RequestBody ForgotNumber number
    ){
      return   mechanicService.forgotpasswords(number.getPhonenumber());
    }

    @PostMapping("api/mechanic/forget/verifytoken")

    public ResponseEntity<?> verifynewpasswordtoken (
            @RequestBody Token token
    ) {
        return mechanicService.verifynewPasswordToken(token);
    }

    @PostMapping("api/mechanic/forget/newPassword")
    public ResponseEntity<?> updatepassword(@RequestBody DtoUser user) {

        return mechanicService.updatePassword(user);

    }


    @PostMapping ("api/mechanic/checknumber")
    public ResponseEntity<?> checkNumber( @RequestBody MechanicNumnerDto numberDto){
        return   mechanicService.checkmechanicnumber (numberDto);

    }
    @GetMapping("api/mechanic/dashboard")
    public ResponseEntity <?> mechanicdashboard (@AuthenticationPrincipal UserDetails userDetails){
        System.out.println("Mecahnic Dashboard Call hogya ");
        String phonenumber =  userDetails.getUsername();
        return mechanicService.mechanicdashboard (phonenumber );

    }


    @PostMapping ("api/mechanic/login")

    public ResponseEntity <?> loginmechanic (@RequestBody MechanicCredientialsDTO credientialsDTO){

        return  mechanicService.loginmechanic (credientialsDTO , authenticationManager);

    }

@PostMapping("api/mechanic/isactive")
    public ResponseEntity<?> isactive ( @RequestBody IsOnlineDto isOnlineDto ,
            @AuthenticationPrincipal UserDetails userDetails){

        String phonenumber  = userDetails.getUsername();
      return  mechanicService.onlinestatus(phonenumber , isOnlineDto);
}

    // MechanicController.java mein add karo
    @GetMapping("/api/mechanic/recent-activity")
    public ResponseEntity<?> getRecentActivity(
            @AuthenticationPrincipal UserDetails userDetails) {

        return  mechanicService.recentactivities (userDetails.getUsername());


    }

    @GetMapping("/api/mechanic/alljobs/history")
    public ResponseEntity<?> alljobshistory(
            @AuthenticationPrincipal UserDetails userDetails) {

        return  mechanicService.alljobshistory (userDetails.getUsername());


    }




    @GetMapping("/api/mechanic/showprofile")
    public ResponseEntity<?> showprofile(
            @AuthenticationPrincipal UserDetails userDetails) {

        return  mechanicService.showmechanicprofile (userDetails.getUsername());


    }




}


