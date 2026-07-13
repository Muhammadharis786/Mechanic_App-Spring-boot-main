package com.haris.MechanicApp.Config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Har STOMP session ID ke against mechanicId store karta hai.
 * CONNECT ke waqt (WebSocketConfig ke interceptor se) add hota hai,
 * aur SessionDisconnectEvent ke waqt (WebSocketPresenceEventListener se)
 * read + remove hota hai — taake disconnect hone pe pata chal sake
 * yeh konsa mechanic tha (app crash / force-kill / net gone).
 */
@Component
public class WebSocketSessionRegistry {

    private final Map<String, Long> sessionIdToMechanicId = new ConcurrentHashMap<>();

    public void register(String sessionId, Long mechanicId) {
        if (sessionId == null || mechanicId == null) return;
        sessionIdToMechanicId.put(sessionId, mechanicId);
    }

    public Long remove(String sessionId) {
        if (sessionId == null) return null;
        return sessionIdToMechanicId.remove(sessionId);
    }

    public Long get(String sessionId) {
        if (sessionId == null) return null;
        return sessionIdToMechanicId.get(sessionId);
    }
}