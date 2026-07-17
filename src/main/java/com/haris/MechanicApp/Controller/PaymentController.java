package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Model.Payment.PaymentRequestDto;
import com.haris.MechanicApp.Model.Payment.SwichTokenResponse;
import com.haris.MechanicApp.Service.PaymentService;
import com.haris.MechanicApp.Utility.SwichChecksumUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class PaymentController {
    @Autowired
    private PaymentService paymentService;
    @Value("${swich.secret-key}")
    private String secretKey;

    @Autowired
    private SwichChecksumUtil checksumUtil;

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

    @GetMapping("/api/payment/callback")

    public ResponseEntity<?> callback(  @RequestParam("PaymentType") String paymentType,
                                        @RequestParam("Status") String status,
                                        @RequestParam("OrderId") String orderId,
                                        @RequestParam("CustomerTransactionId") String customerTransactionId,
                                        @RequestParam("Amount") String amount,
                                        @RequestParam("Checksum") String checksum) throws Exception {
        System.out.println("May Callback URl call hrha hn ");
        System.out.println("Secret key ye hay"+ secretKey);
        String plain = "SWCallback:" + customerTransactionId + ":" + orderId + ":" + amount + ":" + status;
        System.out.println("plain:" + plain);
        String expectedChecksum = checksumUtil.generateCallbackChecksum(
                customerTransactionId, orderId, amount, status, secretKey);

        System.out.println("ye check sum jo meray backend nay generate kya" + expectedChecksum);
        System.out.println("ye check sum jo swich say aya " + checksum);
        Map<String, String> response = new HashMap<>();

        if (!expectedChecksum.equalsIgnoreCase(checksum)) {
            response.put("status", "failed");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }

        response.put("status", "success");
        return ResponseEntity.ok(response);

    }





}
