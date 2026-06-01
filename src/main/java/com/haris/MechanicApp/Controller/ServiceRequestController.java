package com.haris.MechanicApp.Controller;


import com.haris.MechanicApp.Model.Location.LocationDTO;
import com.haris.MechanicApp.Model.RequestService.CreateServiceRequestDto;
import com.haris.MechanicApp.Model.RequestService.SendPriceDto;
import com.haris.MechanicApp.Service.ServiceRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceRequestController {
    @Autowired
    ServiceRequestService serviceRequestService;
    @PostMapping("/api/service-request/create")
    public ResponseEntity<?> createRequest(
            @RequestBody CreateServiceRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails
    )

    {
        return serviceRequestService.createRequest(dto, userDetails.getUsername());
    }

    @PostMapping("/api/service-request/nearbymechanic")
    public ResponseEntity<?> nearbyMechanic(
            @RequestBody LocationDTO dto,
            @AuthenticationPrincipal UserDetails userDetails
    )

    {
        return serviceRequestService.nearbyOnlineMechanics( userDetails.getUsername() , dto);
    }

    @PostMapping("/api/service-request/accept/{requestId}")
    public ResponseEntity<?> acceptRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetails userDetails
    )

    {
        return serviceRequestService.acceptRequest(requestId, userDetails.getUsername());
    }

    @PostMapping("/api/service-request/cancel/{requestId}")
    public ResponseEntity<?> cancelRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetails userDetails
    )

    {
        return serviceRequestService.cancelRequest(requestId, userDetails.getUsername());
    }

    @GetMapping("/api/service-request/tracking/{requestId}")
    public ResponseEntity<?> getRequestTracking(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetails userDetails
    )

    {
        return serviceRequestService.getRequestTracking(requestId, userDetails.getUsername());
    }

    @GetMapping("/api/service-request/isarrived/{requestId}")
    public ResponseEntity<?> checkarrived(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetails userDetails
    )

    {
        return serviceRequestService.checkarrived(requestId, userDetails.getUsername());
    }

    @PostMapping("/api/service-request/send-final-price")
    public ResponseEntity <?>  sendFinalPrice(@RequestBody SendPriceDto dto ,
                                              @AuthenticationPrincipal UserDetails userDetails
    ){
        return  serviceRequestService.sendfinalprice(dto , userDetails.getUsername()) ;
    }

    @PostMapping ("api/service-request/approve-payment-request")
    public  ResponseEntity<?> approvepaymentrequest (@RequestBody SendPriceDto dto ,
                                                     @AuthenticationPrincipal UserDetails userDetails ){

        return  serviceRequestService.approvepaymentrequest (dto  , userDetails.getUsername());
    }

    @GetMapping("api/service-request/approve-payment-request/{requesId}")

    public ResponseEntity<?> workcomplete ( @PathVariable Long requestId,
                                            @AuthenticationPrincipal UserDetails userDetails){


        return serviceRequestService.workcomplete(requestId , userDetails.getUsername());


    }



}
