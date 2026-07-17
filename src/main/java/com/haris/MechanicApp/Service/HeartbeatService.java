package com.haris.MechanicApp.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class HeartbeatService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final long HEARTBEAT_TTL = 60;

    public void heartbeat(Long mechanicId){

            String key = "mechanic:heartbeat:" + mechanicId;




        redisTemplate.opsForValue().set(
                key,
                Instant.now().toString(),
                Duration.ofSeconds(HEARTBEAT_TTL)
        );

    }


}
