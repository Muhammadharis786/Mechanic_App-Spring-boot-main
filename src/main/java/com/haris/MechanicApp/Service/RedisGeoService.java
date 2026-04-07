//package com.haris.MechanicApp.Service;
//
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.connection.RedisGeoCommands;
//import org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs;
//import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
//import org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusResponse;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class RedisGeoService {
//
//    private final RedisTemplate<String, String> redisTemplate;
//
//    public RedisGeoService(RedisTemplate<String, String> redisTemplate) {
//        this.redisTemplate = redisTemplate;
//    }
//
//    // Add mechanic location
//    public void addMechanicLocation(String mechanicId, double latitude, double longitude) {
//        redisTemplate.opsForGeo().add("mechanics", new GeoLocation<>(mechanicId, new RedisGeoCommands.Point(longitude, latitude)));
//    }
//
//    // Get nearby mechanics within radiusKm
//    public List<String> getNearbyMechanics(double userLat, double userLon, double radiusKm) {
//        GeoRadiusCommandArgs args = GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().sortAscending();
//        List<GeoRadiusResponse> responses = redisTemplate.opsForGeo()
//                .radius("mechanics", new RedisGeoCommands.Point(userLon, userLat), radiusKm, RedisGeoCommands.DistanceUnit.KILOMETERS, args);
//
//        return responses.stream()
//                .map(r -> r.getContent().getName())
//                .collect(Collectors.toList());
//    }
//}