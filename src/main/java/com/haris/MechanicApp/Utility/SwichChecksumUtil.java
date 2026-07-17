package com.haris.MechanicApp.Utility;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
public class SwichChecksumUtil {

    // Payment URL generate karte waqt use hota hai (Landing Page / PWA)
    public String generateChecksum(
            String customerTransactionId,
            String item,
            String amount,
            String secretKey) throws Exception {

        String plain = "Swich:" + customerTransactionId + ":" + item + ":" + amount;
        return hmacSha256Hex(plain, secretKey);
    }

    // Callback verify karte waqt use hoga
    public String generateCallbackChecksum(
            String customerTransactionId,
            String orderId,
            String amount,
            String status,
            String secretKey) throws Exception {

        String plain = "SWCallback:" + customerTransactionId + ":" + orderId + ":" + amount + ":" + status;
        return hmacSha256Hex(plain, secretKey);
    }

    // Common HMAC logic — dono methods isay use karenge
    private String hmacSha256Hex(String plain, String secretKey) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(plain.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}