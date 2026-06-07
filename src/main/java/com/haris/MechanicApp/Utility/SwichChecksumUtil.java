package com.haris.MechanicApp.Utility;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

// ChecksumUtil.java
@Component
public class SwichChecksumUtil {

    public String generateChecksum(
            String customerTransactionId,
            String item,
            String amount,
            String secretKey) throws Exception {

        String plain = "Swich:" + customerTransactionId + ":" + item + ":" + amount;

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(plain.getBytes());

        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}