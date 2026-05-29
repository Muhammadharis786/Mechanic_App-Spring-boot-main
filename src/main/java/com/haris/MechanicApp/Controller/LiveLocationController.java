package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Model.GoogleDistance;
import com.haris.MechanicApp.Model.Mechanic.MechanicLiveLocationDto;
import com.haris.MechanicApp.Model.Mechanic.NearbyMechanicLocationDto;
import com.haris.MechanicApp.Model.Request.LiveLocationDto;
import com.haris.MechanicApp.Model.RequestService.AcceptedMechanicLiveLocationDto;
import com.haris.MechanicApp.Model.RequestService.RequestService;
import com.haris.MechanicApp.Model.RoadInfo;
import com.haris.MechanicApp.Repository.ServiceRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Set;

@Controller
public class LiveLocationController {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    /**
     * Flutter Client bhejega: /app/livelocation/{trackingId}
     * Server broadcast karega: /topic/tracking/{trackingId}
     *
     * trackingId = User ka userid hoga (unique session id jaise).
     * User apni location bhejega aur Mechanic is topic ko subscribe karega.
     */
    @MessageMapping("/livelocation/{trackingId}")
    public void handleLiveLocation(
            @DestinationVariable String trackingId,
            LiveLocationDto locationDto) {

        System.out.println("Live location received for trackingId: " + trackingId
                + " | Lat: " + locationDto.getLatitude()
                + " | Lng: " + locationDto.getLongitude());

        // Jis bhi shakhs ne is trackingId ko subscribe kiya hai,
        // usse ye nayi location bhej do (Mechanic ya User dono ke liye kaam karega)
        simpMessagingTemplate.convertAndSend(
                "/topic/tracking/" + trackingId,
                locationDto
        );
    }

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @MessageMapping("/mechanic/live-location")
    public void handleMechanicLiveLocation(MechanicLiveLocationDto dto) {
        System.out.println("Mujhay dto ye mila hay" +dto +"aur id ye hay: "+ dto.getMechanicId());
        String mechanicId = dto.getMechanicId().toString();
        System.out.println("May hit tu hrha hn ");
        redisTemplate.opsForGeo().add(
                "mechanic",
                new Point(dto.getLongitude(), dto.getLatitude()),
                mechanicId
        );

        redisTemplate.opsForHash().put(
                "mechanic:details:" + mechanicId,
                "latitude",
                dto.getLatitude().toString()
        );

        redisTemplate.opsForHash().put(
                "mechanic:details:" + mechanicId,
                "longitude",
                dto.getLongitude().toString()
        );
        // ismay dekh rha hay kay mechacni:map:sessions may  kn kn user dekh rha hay
        // jessay kay mechanic:map-sessions:55 -> "user-map-32 ,"user-map-33 mtlb
        //mechanci 55 ko user 32 aur 33 dekh rhai hain ager mechanci move hoga tu user 32 33 ko dekhaiga
        Set<String> mapSessionIds = redisTemplate.opsForSet().members(
                "mechanic:map-sessions:" + mechanicId
        );

        if (mapSessionIds == null || mapSessionIds.isEmpty()) {
            System.out.println("Mujhay session id nh mili");
        }
        else {
            for (String mapSessionId : mapSessionIds) {

                Boolean isStillInSession = redisTemplate.opsForSet().isMember(
                        "map-session:mechanics:" + mapSessionId,
                        mechanicId
                );

                if (!Boolean.TRUE.equals(isStillInSession)) {
                    continue;
                }

                NearbyMechanicLocationDto locationDto =
                        new NearbyMechanicLocationDto(
                                mapSessionId,
                                dto.getMechanicId(),
                                dto.getLatitude(),
                                dto.getLongitude(),
                                dto.getBearing(),
                                dto.getSpeed()
                        );

                simpMessagingTemplate.convertAndSend(
                        "/topic/nearby-mechanics/" + mapSessionId,
                        locationDto
                );
                System.out.println(mapSessionId);

            }
        }

        sendAcceptedRequestLiveLocation(dto);
    }

    private void sendAcceptedRequestLiveLocation(MechanicLiveLocationDto dto) {
        serviceRequestRepository
                .findActiveAcceptedRequestByMechanicId(dto.getMechanicId())
                .ifPresent(request -> {

                    AcceptedMechanicLiveLocationDto payload =
                            buildAcceptedLiveLocationPayload(request, dto);

                    simpMessagingTemplate.convertAndSend(
                            "/topic/request/"
                                    + request.getRequestId()
                                    + "/live-location",
                            payload
                    );
                });
    }


    private AcceptedMechanicLiveLocationDto buildAcceptedLiveLocationPayload(
            RequestService request,
            MechanicLiveLocationDto dto
    ) {
        String etaKey = "request:eta:" + request.getRequestId();

        Double oldLat = getRedisDouble(etaKey, "lastLat");
        Double oldLng = getRedisDouble(etaKey, "lastLng");
        Long lastCalculatedAt = getRedisLong(etaKey, "lastCalculatedAt");

        Double distance = getRedisDouble(etaKey, "distance");
        String eta = getRedisString(etaKey, "eta");

        boolean shouldRecalculate = shouldRecalculateEta(
                oldLat,
                oldLng,
                lastCalculatedAt,
                dto.getLatitude(),
                dto.getLongitude()
        );

        if (shouldRecalculate) {
            String destination = dto.getLatitude() + "," + dto.getLongitude();

            List<RoadInfo> roadInfos =
                    new GoogleDistance().getBatchRoadDistances(
                            request.getUserLatitude(),
                            request.getUserLongitude(),
                            destination
                    );

            if (!roadInfos.isEmpty() && roadInfos.get(0).getDistance() >= 0) {
                distance = roadInfos.get(0).getDistance();
                eta = roadInfos.get(0).getDistancetime();

                redisTemplate.opsForHash().put(
                        etaKey,
                        "lastLat",
                        dto.getLatitude().toString()
                );

                redisTemplate.opsForHash().put(
                        etaKey,
                        "lastLng",
                        dto.getLongitude().toString()
                );

                redisTemplate.opsForHash().put(
                        etaKey,
                        "lastCalculatedAt",
                        String.valueOf(System.currentTimeMillis())
                );

                redisTemplate.opsForHash().put(
                        etaKey,
                        "distance",
                        distance.toString()
                );

                redisTemplate.opsForHash().put(
                        etaKey,
                        "eta",
                        eta
                );
            }
        }

        return new AcceptedMechanicLiveLocationDto(
                request.getRequestId(),
                dto.getMechanicId(),
                dto.getLatitude(),
                dto.getLongitude(),
                dto.getBearing(),
                dto.getSpeed(),
                distance,
                eta
        );
    }

    private boolean shouldRecalculateEta(
            Double oldLat,
            Double oldLng,
            Long lastCalculatedAt,
            Double newLat,
            Double newLng
    ) {
        if (oldLat == null || oldLng == null || lastCalculatedAt == null) {
            return true;
        }

        long now = System.currentTimeMillis();

        boolean twentySecondsPassed =
                now - lastCalculatedAt >= 20_000;

        double movedMeters = calculateHaversineMeters(
                oldLat,
                oldLng,
                newLat,
                newLng
        );

        boolean movedHundredMeters =
                movedMeters >= 100;

        return twentySecondsPassed || movedHundredMeters;
    }

    private double calculateHaversineMeters(
            double lat1,
            double lon1,
            double lat2,
            double lon2
    ) {
        final int earthRadiusMeters = 6371000;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2)
                        * Math.sin(dLon / 2);

        double c =
                2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusMeters * c;
    }

    private Double getRedisDouble(String key, String field) {
        Object value = redisTemplate.opsForHash().get(key, field);

        if (value == null) {
            return null;
        }

        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Long getRedisLong(String key, String field) {
        Object value = redisTemplate.opsForHash().get(key, field);

        if (value == null) {
            return null;
        }

        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String getRedisString(String key, String field) {
        Object value = redisTemplate.opsForHash().get(key, field);
        return value == null ? null : value.toString();
    }

}
