package com.haris.MechanicApp.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {


  @Value("${safepay.public.key}")
    private String publickey;
    @Value("${safepay.secret.key}")
    private String secretkey;
    @Value("${safepay.api.baseurl}")
    private String baseUrl;
    public String createOrder(Double amount, String appointmentId) {
        RestTemplate restTemplate = new RestTemplate();
        String url = baseUrl + "/order/v1/init";
        Map<String, Object> request = new HashMap<>();
        request.put("client", publickey); // ✓ Sahi (Yahan apka Public Key lagega)   // important (docs)
        request.put("amount", amount);
        request.put("currency", "PKR");
        request.put("environment", "sandbox");

        System.out.println("Base URL: " + baseUrl);
        System.out.println("Public Key (Client ID): " + publickey);
        System.out.println("Secret Key (Header): " + secretkey);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.set("X-SFPY-MERCHANT-SECRET", secretkey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        try
        {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
            Map<String, Object> body = response.getBody();

            Map<String, Object> data = (Map<String, Object>) body.get("data");
            String token = (String) data.get("token");
            return "https://sandbox.api.getsafepay.com/components?token=" + token + "&env=sandbox";
        }


        catch (Exception e) {
            // Agar koi error aaye (jaise network ya bad request) to yahan pakra jaye
            throw new RuntimeException("Safepay session creation failed: " + e.getMessage());
        }



    }


}
