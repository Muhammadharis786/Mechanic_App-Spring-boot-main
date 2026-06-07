package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Model.Payment.PaymentRequestDto;
import com.haris.MechanicApp.Model.Payment.SwichTokenResponse;
import com.haris.MechanicApp.Service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @GetMapping("api/payment/get-token")
    public String getToken() {
        return paymentService.getAccessToken();
    }

    @PostMapping("/generate-url")
    public ResponseEntity<?> generatePaymentUrl(@RequestBody PaymentRequestDto dto) {
        try {
            String paymentUrl = paymentService.generatePaymentUrl(
                    dto.getPayeeName(),
                    dto.getEmail(),
                    dto.getMsisdn(),
                    dto.getItem(),
                    dto.getAmount()
            );

            Map<String, String> response = new HashMap<>();
            response.put("paymentUrl", paymentUrl);
            response.put("message", "Payment URL generated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Error generating payment URL: " + e.getMessage());
        }
    }

}
