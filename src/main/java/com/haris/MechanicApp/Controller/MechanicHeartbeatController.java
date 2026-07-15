package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Service.MechanicPresenceHeartbeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MechanicHeartbeatController {

    @Autowired
    private MechanicPresenceHeartbeatService heartbeatService;

    // Mechanic app har 30s pe yeh hit karega jab tak online hay
    @PostMapping("/api/mechanic/heartbeat")
    public ResponseEntity<?> heartbeat(@RequestParam Long mechanicId) {
        heartbeatService.recordHeartbeat(mechanicId);
        return ResponseEntity.ok().build();
    }
}