package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Model.Appointments.AutoAppointmentDto;
import com.haris.MechanicApp.Model.Appointments.ManualAppointmentDto;
import com.haris.MechanicApp.Model.Appointments.ReasonDTO;
import com.haris.MechanicApp.Model.Location.Location;
import com.haris.MechanicApp.Service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
public class AppointmentController {


    @Autowired
    private AppointmentService appointmentService;
    @PostMapping("api/user/bookappointment/nearbymechanics")
    public ResponseEntity<?> nearbymechanics(

            @AuthenticationPrincipal UserDetails userDetails,
          @RequestBody  Location location
    ){
        String userphonenumber = userDetails.getUsername();


        return appointmentService.nearbymechanics (userphonenumber  , location);

    }

    @PostMapping("api/user/auto/bookappointment")
    public ResponseEntity<?> autoAppointment(
            @RequestBody AutoAppointmentDto appointmentDto ,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        String userphonenumber = userDetails.getUsername();


        return appointmentService.autobookappointment (userphonenumber ,appointmentDto);

    }
    @PostMapping ("api/user/manual/bookappointment")
    public ResponseEntity<?> manualAppointment(
            @RequestBody ManualAppointmentDto appointmentDto ,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        String userphonenumber = userDetails.getUsername();


        return appointmentService.manualbookappointment (userphonenumber ,appointmentDto);

    }
        @GetMapping ("api/mechanic/appointments/allnotifications")
    public ResponseEntity<?> allnotifications(
            @AuthenticationPrincipal UserDetails userDetails
    ){
        String userphonenumber = userDetails.getUsername();
        return appointmentService.mechanicallnotifications (userphonenumber);
    }
    @GetMapping ("api/user/appointments/allnotifications")
    public ResponseEntity<?> allnotificationsusers(
            @AuthenticationPrincipal UserDetails userDetails
    ){
        String userphonenumber = userDetails.getUsername();
        return appointmentService.userallnotifications (userphonenumber);
    }


    @GetMapping("api/mechanic/appointments/isread/{notificationid}")
    public void isread(@PathVariable("notificationid") long notificationid,
                                    @AuthenticationPrincipal UserDetails userDetails){
        String userphonenumber = userDetails.getUsername();
        appointmentService.isreadnotification(userphonenumber , notificationid);

    }

    @GetMapping("api/user/appointments/isread/{notificationid}")
    public void isreaduser(
            @PathVariable("notificationid") long notificationid,
                       @AuthenticationPrincipal UserDetails userDetails){
        String userphonenumber = userDetails.getUsername();
        appointmentService.isreadnotificationuser(userphonenumber , notificationid);

    }
    @GetMapping("api/user/appointments/showuserappointments")
    public ResponseEntity<?> showuserappointments(@AuthenticationPrincipal UserDetails userDetails){
        String userphonenumber = userDetails.getUsername();
       return appointmentService.showuserappointments(userphonenumber);

    }

    @GetMapping("api/mechanic/appointments/showmechanicappointments")
    public ResponseEntity<?> showmechanicappointments(@AuthenticationPrincipal UserDetails userDetails){
        String userphonenumber = userDetails.getUsername();
        return appointmentService.showmechanicappointments(userphonenumber);

    }
    @PostMapping ("api/mechanic/appointment/rejectappointment/{appointmentid}")
    public ResponseEntity<?> rejectappointment(@AuthenticationPrincipal UserDetails userDetails,
                                               @PathVariable ("appointmentid") String appointmentid,
                                           @RequestBody    ReasonDTO reasonDTO){
        String mechanicphonenumber = userDetails.getUsername();
        return appointmentService.rejectappointment(mechanicphonenumber ,appointmentid ,reasonDTO);
    }

    @PostMapping ("api/user/appointment/cancelappointment/{appointmentid}")
    public ResponseEntity<?> cancelappointment(@AuthenticationPrincipal UserDetails userDetails,
                                               @PathVariable ("appointmentid") String appointmentid,
                                               @RequestBody    ReasonDTO reasonDTO){
        String phonenumber = userDetails.getUsername();
        return appointmentService.cancelappointment(phonenumber ,appointmentid ,reasonDTO);
    }

    @GetMapping ("api/user/appointment/acceptappointment/{appointmentid}")
    public ResponseEntity<?> acceptappointment(
                                                @AuthenticationPrincipal UserDetails userDetails,
                                               @PathVariable ("appointmentid") String appointmentid
                                             ){
        String phonenumber = userDetails.getUsername();
        return appointmentService.acceptappointment(phonenumber ,appointmentid );
    }

    @GetMapping ("api/mechanic/appointment/startappointment/{appointmentid}")
    public ResponseEntity<?> startappointment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable ("appointmentid") String appointmentid
    ){
        String phonenumber = userDetails.getUsername();
        return appointmentService.startappointment(phonenumber ,appointmentid );
    }

}
