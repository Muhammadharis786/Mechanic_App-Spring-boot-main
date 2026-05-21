package com.haris.MechanicApp.Config;



import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import org.springframework.beans.factory.annotation.Value;


import jakarta.annotation.PostConstruct;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account}")
    private Resource serviceAccount;

    @PostConstruct
    public void initializeFirebase() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return;
            }

            try (InputStream inputStream = serviceAccount.getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(inputStream))
                        .build();

                FirebaseApp.initializeApp(options);
            }

            System.out.println("Firebase Admin initialized successfully");
        } catch (Exception e) {
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }
}