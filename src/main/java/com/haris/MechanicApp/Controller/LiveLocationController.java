package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Model.Mechanic.MechanicLiveLocationDto;
import com.haris.MechanicApp.Model.Mechanic.NearbyMechanicLocationDto;
import com.haris.MechanicApp.Model.Request.LiveLocationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Set;

@Controller
public class LiveLocationController {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

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
            return;
        }

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
}
