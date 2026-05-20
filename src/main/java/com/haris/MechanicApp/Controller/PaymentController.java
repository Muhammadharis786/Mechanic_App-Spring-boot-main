package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Model.Payment.PaymentRequest;
import com.haris.MechanicApp.Model.Payment.PaymentResponse;
import com.haris.MechanicApp.Service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
}
