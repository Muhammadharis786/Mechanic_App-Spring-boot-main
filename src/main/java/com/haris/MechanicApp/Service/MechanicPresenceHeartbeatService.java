package com.haris.MechanicApp.Service;

import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Repository.MechanicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Self-healing safety net — WebSocket disconnect event aur STOMP heartbeat
 * dono hi "best effort" hain, kabhi miss ho sakte hain (network edge cases).
 * Yeh scheduled job ensure karta hay ke chahe wo dono miss ho jayen,
 * mechanic zyada se zyada ~90s baad khud offline mark ho jaye ga
 * agar uska app heartbeat bhejna band kar de.
 *
 * Client side: har 30s pe /api/mechanic/heartbeat hit karo jab tak
 * mechanic online + app foreground/background (but not killed) hay.
 */
@Service
public class MechanicPresenceHeartbeatService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private MechanicRepository mechanicRepository;

    private static final long HEARTBEAT_TTL_SECONDS = 90; // client har 30s pe refresh karega

    /** Client isay call karega /api/mechanic/heartbeat endpoint se. */
    public void recordHeartbeat(Long mechanicId) {
        redisTemplate.opsForValue().set(
                "mechanic:heartbeat:" + mechanicId,
                "1",
                HEARTBEAT_TTL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    /**
     * Har 30s check karta hay: jo mechanic Redis mein "online" hain,
     * unka heartbeat key abhi bhi zinda hay ya expire ho chuka?
     * Expire ho chuka tu matlab uska connection kabhi ka mar chuka
     * lekin disconnect event miss ho gaya — force offline karo.
     */
    @Scheduled(fixedRate = 30000)
    public void expireStaleMechanics() {
        Set<String> onlineKeys = redisTemplate.keys("mechanic:online:*");
        if (onlineKeys == null) return;

        for (String key : onlineKeys) {
            String mechanicIdStr = key.substring("mechanic:online:".length());
            Boolean hasHeartbeat = redisTemplate.hasKey("mechanic:heartbeat:" + mechanicIdStr);

            if (Boolean.FALSE.equals(hasHeartbeat)) {
                Long mechanicId = Long.parseLong(mechanicIdStr);
                markOffline(mechanicId);
                System.out.println("Mechanic " + mechanicId + " heartbeat expired — force marked offline");
            }
        }
    }

    private void markOffline(Long mechanicId) {
        mechanicRepository.findById(mechanicId).ifPresent(mech -> {
            mech.setIsactive(false);
            mechanicRepository.save(mech);
        });
        redisTemplate.delete("mechanic:online:" + mechanicId);
        redisTemplate.opsForHash().put("mechanic:details:" + mechanicId, "isOnline", "false");
    }
}