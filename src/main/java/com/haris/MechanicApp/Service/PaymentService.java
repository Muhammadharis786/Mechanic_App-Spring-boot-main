package com.haris.MechanicApp.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class PaymentService {

    @Value("${safepay.public.key}")
    private String publicKey;

    @Value("${safepay.secret.key}")
    private String secretKey;

    @Value("${safepay.api.baseurl}")
    private String baseUrl;

    @Value("${safepay.environment:sandbox}")
    private String environment;

    @Value("${safepay.checkout.source:mobile}")
    private String checkoutSource;

    @Value("${safepay.redirect.url:https://example.com/payment/success}")
    private String redirectUrl;

    @Value("${safepay.cancel.url:https://example.com/payment/cancel}")
    private String cancelUrl;

    public String createOrder(Double amount, String appointmentId) {
        try {
            String tracker = createPaymentSession(amount);
            String tbt = createAuthenticationToken();

            return buildCheckoutUrl(tracker, tbt);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String createPaymentSession(Double amount) {
        RestTemplate restTemplate = new RestTemplate();

        String url = baseUrl + "/order/payments/v3/";

        Map<String, Object> request = Map.of(
                "merchant_api_key", publicKey,
                "intent", "CYBERSOURCE",
                "mode", "payment",
                "entry_mode", "raw",
                "currency", "PKR",
                "amount", toLowestDenomination(amount),
                "include_fees", false
        );

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(request, safepayHeaders());

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
        );

        Map<String, Object> body = response.getBody();
        if (body == null || body.get("data") == null) {
            throw new RuntimeException("Safepay payment session response is empty");
        }

        Map<String, Object> data = (Map<String, Object>) body.get("data");
        Map<String, Object> tracker = (Map<String, Object>) data.get("tracker");

        if (tracker == null || tracker.get("token") == null) {
            throw new RuntimeException("Safepay tracker token not found");
        }

        return tracker.get("token").toString();
    }

    private String createAuthenticationToken() {
        RestTemplate restTemplate = new RestTemplate();

        String url = baseUrl + "/client/passport/v1/token";

        HttpEntity<Void> entity = new HttpEntity<>(safepayHeaders());

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
        );

        Map<String, Object> body = response.getBody();
        if (body == null || body.get("data") == null) {
            throw new RuntimeException("Safepay authentication token response is empty");
        }

        return body.get("data").toString();
    }

    private String buildCheckoutUrl(String tracker, String tbt) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl + "/components")
                .queryParam("beacon", tracker)
                .queryParam("tracker", tracker)
                .queryParam("tbt", tbt)
                .queryParam("env", environment)
                .queryParam("environment", environment)
                .queryParam("source", checkoutSource);

        if ("hosted".equalsIgnoreCase(checkoutSource)) {
            builder
                    .queryParam("redirect_url", redirectUrl)
                    .queryParam("cancel_url", cancelUrl);
        }

        return builder
                .build()
                .encode()
                .toUriString();
    }

    private HttpHeaders safepayHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-SFPY-MERCHANT-SECRET", secretKey);
        return headers;
    }

    private Long toLowestDenomination(Double amount) {
        return Math.round(amount * 100);
    }

    public ResponseEntity<?> generatetoken(String phonenumber) {

        String payfastUrl = "https://sandbox.gopayfast.com/token";
        RestTemplate restTemplate = new RestTemplate();

        // 1. PayFast ko bhejney ke liye headers tayar karein
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 2. PayFast ke required parameters (Form Data)
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("merchant_id", "14833");
        formData.add("secured_key", "rPcy4T7GQkSCFsHBLdn26s");
        formData.add("grant_type", "client_credentials");
        formData.add("customer_ip", "127.0.0.1"); // Aap yahan user ka real IP bhi pass kar sakte hain

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);

        try {
            // 3. Spring Boot khud PayFast ki API ko POST request maarega
            ResponseEntity<Map> response = restTemplate.postForEntity(payfastUrl, requestEntity, Map.class);

            // 4. PayFast se jo bhi JSON response (token, expiry etc.) aya, wo wapas Flutter ko return kar dein
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "PayFast token generation failed: " + e.getMessage()));
        }
    }
}