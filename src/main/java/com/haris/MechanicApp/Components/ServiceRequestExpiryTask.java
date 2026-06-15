package com.haris.MechanicApp.Components;

import com.haris.MechanicApp.Model.RequestService.RequestService;
import com.haris.MechanicApp.Model.RequestService.ServiceRequestStatus;
import com.haris.MechanicApp.Repository.ServiceRequestRepository;
import com.haris.MechanicApp.Service.FcmService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ServiceRequestExpiryTask {

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private FcmService fcmService;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;  // WebSocket ke liye

    @Scheduled(fixedRate = 60_000)  // har 1 minute mein
    @Transactional
    public void expireUnacceptedRequests() {

        // 5 minute pehle ka cutoff
        Instant cutoff = Instant.now().minus(5, ChronoUnit.MINUTES);

        List<RequestService> expiredRequests =
                serviceRequestRepository.findExpiredPendingRequests(cutoff);

        if (expiredRequests.isEmpty()) return;

        for (RequestService request : expiredRequests) {

            // 1. Status EXPIRED karo
            request.setRequestStatus(ServiceRequestStatus.EXPIRED);
            serviceRequestRepository.save(request);

            if (request.getUser() == null) continue;

            // 2. FCM Push Notification (background mein bhi aata hai)
            Map<String, String> data = new HashMap<>();
            data.put("requestId", String.valueOf(request.getRequestId()));
            data.put("type", "REQUEST_EXPIRED");

            fcmService.sendToUser(
                    request.getUser(),
                    "Request Expire Ho Gayi",
                    "Koi mechanic available nahi tha. Dobara 20 minute bad try karein.",
                    data
            );

            // 3. WebSocket Notification (app khula ho to real-time update)
            Map<String, Object> wsPayload = new HashMap<>();
            wsPayload.put("requestId", request.getRequestId());
            wsPayload.put("status", "EXPIRED");
            wsPayload.put("message", "Koi mechanic available nahi tha. Dobara 20 minute bad try karein.");

            simpMessagingTemplate.convertAndSend(
                    "/topic/request-status/" + request.getUser().getUserid(),
                    (Object) wsPayload
            );

            System.out.println("[ExpiryTask] Request #" + request.getRequestId()
                    + " EXPIRED — FCM + WebSocket bheja gaya.");
        }
    }
}