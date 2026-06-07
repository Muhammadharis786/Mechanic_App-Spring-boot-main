package com.haris.MechanicApp.Service;

import com.haris.MechanicApp.Model.Appointments.Appointments;
import com.haris.MechanicApp.Model.Payment.PaymentResponseDto;
import com.haris.MechanicApp.Model.Payment.SwichTokenResponse;
import com.haris.MechanicApp.Repository.AppointmentRepository;
import com.haris.MechanicApp.Utility.SwichChecksumUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    @Value("${swich.client-id}")
    private String clientId;

    @Value("${swich.secret-key}")
    private String secretKey;

    @Value("${swich.pwa-url}")
    private String pwaUrl;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private SwichChecksumUtil checksumUtil;


    private final RestTemplate restTemplate = new RestTemplate();

    public String getAccessToken() {
        String authUrl = "https://sandbox-auth.swichnow.com/connect/token" ;
        MultiValueMap<String, String> body =
                new LinkedMultiValueMap<>();

        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", secretKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED
        );

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<SwichTokenResponse> response =
                restTemplate.exchange(
                        authUrl,
                        HttpMethod.POST,
                        request,
                        SwichTokenResponse.class
                );

        return response.getBody().getAccess_token();
    }
    public String generatePaymentUrl(
            String payeeName,
            String email,
            String msisdn,
            String item,
            String amount) throws Exception {

        // Unique transaction ID
        String txnId = "TXN" + System.currentTimeMillis();

        // Checksum generate karo
        String checksum = checksumUtil.generateChecksum(txnId, item, amount, secretKey);

        // PWA URL banao
        String paymentUrl = pwaUrl + "?"
                + "clientId=" + clientId
                + "&customerTransactionId=" + txnId
                + "&item=" + URLEncoder.encode(item, "UTF-8")
                + "&amount=" + 10
                + "&channel=0"
                + "&description=MechanicServicePayment"
                + "&PayeeName=" + URLEncoder.encode(payeeName, "UTF-8")
                + "&Email=" + URLEncoder.encode(email, "UTF-8")
                + "&MSISDN=" + msisdn
                + "&currency=PKR"
                + "&billReferenceNo=" + txnId
                + "&checksum=" + checksum;

        return paymentUrl;
    }
}