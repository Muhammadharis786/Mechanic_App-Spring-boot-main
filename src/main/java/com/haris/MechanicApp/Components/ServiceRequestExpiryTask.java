package com.haris.MechanicApp.Components;

import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.RequestService.RequestService;
import com.haris.MechanicApp.Model.RequestService.ServiceRequestStatus;
import com.haris.MechanicApp.Repository.MechanicRepository;
import com.haris.MechanicApp.Repository.ServiceRequestRepository;
import com.haris.MechanicApp.Service.FcmService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class ServiceRequestExpiryTask {

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private FcmService fcmService;

    @Autowired
    private MechanicRepository mechanicRepository;


    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;  // WebSocket ke liye


    @Autowired
    private  RedisTemplate<String, String> redisTemplate;

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

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void updateMechanicPresence() {

        Set<String> keys = redisTemplate.keys("mechanic:details:*");
        List<Long> mechanicIds = new ArrayList<>();
        if (keys != null) {
            for (String key : keys) {

                Long mechanicId = Long.parseLong(
                        key.replace("mechanic:details:", "")
                );

                String currentStatus = (String) redisTemplate.opsForHash()
                        .get(key, "isOnline");
                if (currentStatus == null) {
                    continue;
                }
                boolean heartbeatExists = Boolean.TRUE.equals(
                        redisTemplate.hasKey("mechanic:heartbeat:" + mechanicId)
                );

                String newStatus = heartbeatExists ? "true" : "false";
                // Agar status change hi nahi hua to kuch mat karo
                if (currentStatus.equals(newStatus)) {
                    System.out.println("phelay is id "+ mechanicId + " ka status tha "+ currentStatus +
                            " ab hay "+ newStatus + " tu no update");
                    mechanicIds.add(mechanicId);
                    continue;
                }
                System.out.println("phelay is id "+ mechanicId + " ka status tha "+ currentStatus +
                        " ab hay "+ newStatus + " tu  update");
                // Redis Update
                redisTemplate.opsForHash().put(
                        key,
                        "isOnline",
                        newStatus
                );

                // Database Update
                mechanicRepository.findById(mechanicId)
                        .ifPresent(mech -> {
                            mech.setIsactive(heartbeatExists);
                            mechanicRepository.save(mech);
                        });

                mechanicIds.add(mechanicId);


            }
            System.out.println("mechanicIds  = " + mechanicIds);
        }
    }
}