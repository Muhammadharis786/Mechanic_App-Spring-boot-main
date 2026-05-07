package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Model.Appointments.AutoAppointmentDto;
import com.haris.MechanicApp.Model.Appointments.ManualAppointmentDto;
import com.haris.MechanicApp.Model.Location.Location;
import com.haris.MechanicApp.Service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
}
