package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Model.Mechanic.HeartbeatDTO;
import com.haris.MechanicApp.Service.HeartbeatService;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
public class HeartbeatController {


    @Autowired
    private  HeartbeatService heartbeatService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    @MessageMapping("/heartbeat")
    public void heartbeat(HeartbeatDTO dto) {

        heartbeatService.heartbeat(dto.getMechanicId());

    }


}
