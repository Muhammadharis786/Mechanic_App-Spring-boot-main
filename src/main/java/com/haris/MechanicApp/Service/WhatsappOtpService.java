package com.haris.MechanicApp.Service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;


@Service
public class WhatsappOtpService {
    @Value("${verifyway.api.url}")
    private String apiUrl;

    @Value("${verifyway.api.key}")
    private String apiKey;






    public String sendwhatsappotp(String phoneNumber, String otp) {

        RestTemplate restTemplate = new RestTemplate();
        System.out.println("sendwhatsappotp");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        String body = """
        {
            "recipient": "%s",
            "type": "otp",
            "code": "%s",
            "channel": "whatsapp"
        }
        """.formatted(phoneNumber, otp);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                restTemplate.postForEntity(apiUrl, entity, String.class);

        return response.getBody();
    }
}
