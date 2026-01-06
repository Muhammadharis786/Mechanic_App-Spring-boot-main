package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Mechanic.MechanicCredientialsDTO;
import com.haris.MechanicApp.Model.Mechanic.MechanicNumnerDto;
import com.haris.MechanicApp.Model.Mechanic.MechanicRegistrationDto;
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

    @PostMapping(value = "api/mechanic/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerMechanic(
            @RequestPart("userData") MechanicRegistrationDto mechanicdata,
            @RequestPart("mechanicprofilePicture") MultipartFile mecanicimg,
            @RequestPart("cnicfrontimg") MultipartFile cnicfrontimg,
            @RequestPart("cnicbackimg") MultipartFile cnicbackimg

            ){
        return  mechanicService.registerMechanic (mechanicdata , mecanicimg , cnicbackimg ,cnicfrontimg);


    }

    @PostMapping ("api/mechanic/checknumber")
    public ResponseEntity<?> checkNumber(MechanicNumnerDto numberDto){
        return   mechanicService.checkmechanicnumber (numberDto);

    }

    @PostMapping ("api/mechanic/login")

    public ResponseEntity <?> loginmechanic (@RequestBody MechanicCredientialsDTO credientialsDTO){

        return  mechanicService.loginmechanic (credientialsDTO , authenticationManager);

    }

    @GetMapping("api/mechanic/dashboard")
    public ResponseEntity <?> mechanicdashboard (@AuthenticationPrincipal UserDetails userDetails){
        String phonenumber =  userDetails.getUsername();



        return mechanicService.mechanicdashboard (phonenumber );

    }
}


