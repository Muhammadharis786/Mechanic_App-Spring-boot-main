package com.haris.MechanicApp.Service;

import com.google.firebase.messaging.*;
import com.haris.MechanicApp.Model.FCM.FcmToken;
import com.haris.MechanicApp.Model.FCM.FcmTokenRequestDTO;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Verification.User;
import com.haris.MechanicApp.Repository.FcmTokenRepository;
import com.haris.MechanicApp.Repository.MechanicRepository;
import com.haris.MechanicApp.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class FcmService {

    @Autowired
    private FcmTokenRepository fcmTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MechanicRepository mechanicRepository;

    @Transactional

    // ye tab call hoga jab user or mechanci loign kraingay aur apnay mobile ka token mujhay dainga
    public void saveToken(String phonenumber, String role, FcmTokenRequestDTO request) {
        String token = request.getFcmToken();

        if (token == null || token.isBlank()) {
            throw new RuntimeException("FCM token is required");
        }

        String normalizedRole = role != null ? role : request.getUserType();
        if (normalizedRole == null) {
            normalizedRole = "USER";
        }
        normalizedRole = normalizedRole.toUpperCase();
        System.out.println("normalized role: " + normalizedRole);

        FcmToken fcmToken = fcmTokenRepository.findByToken(token)
                .orElseGet(FcmToken::new);  //this is check token ager nh so new token create kraiga

        fcmToken.setToken(token);
        fcmToken.setPlatform(request.getPlatform());
        fcmToken.setUserType(normalizedRole);
        fcmToken.setUpdatedAt(Instant.now());

        if (fcmToken.getCreatedAt() == null) {
            fcmToken.setCreatedAt(Instant.now());
        }

        if ("MECHANIC".equals(normalizedRole)) {
            Mechanic mechanic = mechanicRepository.findByPhonenumber(phonenumber)
                    .orElseThrow(() -> new RuntimeException("Mechanic not found"));

           fcmToken.setMechanic(mechanic);
            fcmToken.setUser(null);
        } else {
            User user = userRepository.findByPhonenumber(phonenumber)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            fcmToken.setUser(user);
            fcmToken.setMechanic(null);
        }

        fcmTokenRepository.save(fcmToken);
    }

    public void sendToUser(User user, String title, String body, Map<String, String> data) {
        List<FcmToken> tokens = fcmTokenRepository.findByUser(user);
        sendToTokens(tokens, title, body, data);
    }

    public void sendToMechanic(Mechanic mechanic, String title, String body, Map<String, String> data) {
        List<FcmToken> tokens = fcmTokenRepository.findByMechanic(mechanic);
        sendToTokens(tokens, title, body, data);
    }

    private void sendToTokens(List<FcmToken> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        for (FcmToken fcmToken : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(fcmToken.getToken())
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putAllData(data != null ? data : new HashMap<>())
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .setNotification(AndroidNotification.builder()
                                        .setChannelId("onfix_high_importance")
                                        .setSound("default")
                                        .setIcon("ic_launcher")
                                        .build())
                                .build())
                        .build();


                String response = FirebaseMessaging.getInstance().send(message);
                System.out.println("FCM sent successfully: " + response);
            } catch (FirebaseMessagingException e) {
                System.out.println("FCM send failed: " + e.getMessagingErrorCode() + " token=" + fcmToken.getToken());

                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
                        e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                    fcmTokenRepository.delete(fcmToken);
                }
            } catch (Exception e) {
                System.out.println("FCM unexpected error: " + e.getMessage());
            }
        }
    }
}