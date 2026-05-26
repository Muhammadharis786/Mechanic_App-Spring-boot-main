package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Model.Payment.PaymentRequest;
import com.haris.MechanicApp.Model.Payment.PaymentResponse;
import com.haris.MechanicApp.Service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpHeaders;

@RestController
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

  @PostMapping  ("/api/payment/create-session")

  public PaymentResponse createSession(@RequestBody PaymentRequest request) {

      String checkoutUrl = paymentService.createOrder(
              request.getAmount(),
              request.getAppointmentId()
      );

      return new PaymentResponse(checkoutUrl);
  }




    @PostMapping("api/token")
    public ResponseEntity<?> tokengenerate (@AuthenticationPrincipal UserDetails userDetails){
      String phonenumber = userDetails.getUsername();
       return paymentService.generatetoken (phonenumber);

    }




}
