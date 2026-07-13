package com.haris.MechanicApp.Config;

import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Repository.MechanicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Optional;

/**
 * Backup safety net for mechanic online/offline status.
 *
 * Normal flow: mechanic manually toggles online/offline (MechanicController
 * /api/mechanic/isactive), aur Flutter side "detached" lifecycle state pe
 * bhi offline bhejta hai jab app poori tarah close ho.
 *
 * Masla: agar app crash ho jaye, user force-swipe kar ke band kare, ya
 * phone ka low-memory killer app ko kill kar de — "detached" state fire
 * hone ka waqt hi nahi milta, is liye Flutter kabhi offline API call nahi
 * kar pata. Us case may WebSocket ka TCP connection khud-ba-khud drop hota
 * hai — Spring yahan SessionDisconnectEvent fire karta hai, chahe wajah
 * kuch bhi ho. Isi event ko backup ke tor pe use kar rahe hain taake
 * mechanic hamesha Redis/DB may "online" phasa na rahe.
 */
@Component
public class WebSocketPresenceEventListener {

    @Autowired
    private WebSocketSessionRegistry sessionRegistry;

    @Autowired
    private MechanicRepository mechanicRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        Long mechanicId = sessionRegistry.remove(sessionId);
        if (mechanicId == null) {
            return; // yeh session kisi mechanic se linked nahi tha (ya user app tha)
        }

        markMechanicOffline(mechanicId);
    }

    private void markMechanicOffline(Long mechanicId) {
        // DB
        Optional<Mechanic> mechOpt = mechanicRepository.findById(mechanicId);
        mechOpt.ifPresent(mech -> {
            mech.setIsactive(false);
            mechanicRepository.save(mech);
        });

        // Redis — same do keys jo manual toggle (MechanicService.onlinestatus)
        // update karta hai, taake GeoSearch/dashboard filtering consistent rahe
        redisTemplate.delete("mechanic:online:" + mechanicId);
        redisTemplate.opsForHash().put(
                "mechanic:details:" + mechanicId,
                "isOnline",
                "false"
        );

        System.out.println("Mechanic " + mechanicId + " disconnected (WebSocket) — marked offline");
    }
}