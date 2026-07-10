package com.haris.MechanicApp.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Point;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.awt.*;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;


public class GoogleDistance {


    private String apiKey ="eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjRmZmQ5OGVmYTVlNDRjNDRhNGViYTlhN2JmNmIzNzJlIiwiaCI6Im11cm11cjY0In0=";

    private static final String API_KEY = "AIzaSyBpyZg2i30gOLUKK0furYdGDbWXe4lqpkU";
    private static final String BASE_URL = "https://maps.googleapis.com/maps/api/directions/json";

    public String directionapi(double userLatitude , double userLongitude
                                    , double mechlat ,double mechlong)
    {
        RestTemplate restTemplate = new RestTemplate();

        String url = UriComponentsBuilder.newInstance() .
                scheme("https")
                .host("maps.googleapis.com")
                .path("/maps/api/directions/json")

                .queryParam("origin", userLatitude + "," + userLongitude)
                .queryParam("destination", mechlat + "," + mechlong)
                .queryParam("mode", "driving")
                .queryParam("key", API_KEY)
                .toUriString();

        System.out.println("Calling URL: " + url);
        // Raw JSON response as String — taake tum poora response dekh sako
        String response = restTemplate.getForObject(url, String.class);

        System.out.println("Response: " + response);

        return  response;
    }

    public List<RoadInfo> getBatchRoadDistances(double userLatitude, double userLongitude, String destinationsParam) {
        List<RoadInfo> roadInfos = new ArrayList<>();

        if (destinationsParam == null || destinationsParam.isEmpty()) {
            return roadInfos;
        }

        try {
            String originLocation = userLatitude + "," + userLongitude;

            URI uri = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host("maps.googleapis.com")
                    .path("/maps/api/distancematrix/json")
                    .queryParam("origins", originLocation)
                    .queryParam("destinations", destinationsParam)
                    .queryParam("key", API_KEY)
                    .build()
                    .toUri();

            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(uri, String.class);

            System.out.println("Google API Raw Response: " + response); // Debug ke liye

            JSONObject json = new JSONObject(response);

            // FIX: Top-level status check pehle karo
            String topStatus = json.optString("status", "UNKNOWN");
            if (!topStatus.equals("OK")) {
                System.err.println("Google Distance Matrix API failed. Status: " + topStatus);
                return roadInfos; // khaali list return karo, crash mat hone do
            }

            JSONArray rows = json.getJSONArray("rows");

            // FIX: rows empty hone ka case handle karo
            if (rows.length() == 0) {
                System.err.println("No rows returned from Google API");
                return roadInfos;
            }

            JSONArray elementsArray = rows.getJSONObject(0).getJSONArray("elements");

            for (int i = 0; i < elementsArray.length(); i++) {
                JSONObject element = elementsArray.getJSONObject(i);

                String elementStatus = element.optString("status", "UNKNOWN");
                if (elementStatus.equals("OK")) {
                    int distanceMeters = element.getJSONObject("distance").getInt("value");
                    String eta = element.getJSONObject("duration").getString("text");
                    double distanceKm = (double) distanceMeters / 1000.0;
                    roadInfos.add(new RoadInfo(distanceKm, eta));
                } else {
                    roadInfos.add(new RoadInfo(-1, "NULL"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return roadInfos;
    }


    public      float CalulateDistance (BigDecimal mechlatitude, BigDecimal mechlongitude ,
                            BigDecimal userlatitude , BigDecimal userlongitude ){

        // Check for null coordinates to avoid issues
        if (mechlatitude == null || mechlongitude == null || userlatitude == null || userlongitude == null) {
            return -1;
        }

        String mechaniclocation = String.valueOf(mechlatitude +"," + mechlongitude );
        String userlocation = String.valueOf(userlatitude + "," + userlongitude );
        String url = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host("maps.googleapis.com")
                .path("/maps/api/distancematrix/json")
                .queryParam("origins", mechaniclocation)          // origin parameter add
                .queryParam("destinations", userlocation) // destination parameter add
                .queryParam("key", API_KEY)
                .toUriString();

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);
        JSONObject json = new JSONObject(response);

        JSONObject element = json.getJSONArray("rows")
                .getJSONObject(0)
                .getJSONArray("elements")
                .getJSONObject(0);

        int distanceMeters = element.getJSONObject("distance").getInt("value");
        String distanceText = element.getJSONObject("distance").getString("text");

            float distanceinkm =  Float.parseFloat(String.valueOf(distanceMeters))/1000;


        return distanceinkm;



    }
//ye location name nikalnay k lie
    public String getAddressFromLatLng(double latitude, double longitude) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng="
                    + latitude + "," + longitude
                    + "&key=" + API_KEY;

            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = new JSONObject(response);

            // Safety check: if results array is empty
            if (!json.getJSONArray("results").isEmpty()) {
                String address = json.getJSONArray("results")
                        .getJSONObject(0)
                        .getString("formatted_address");
                return address;
            } else {
                System.out.println("No address found for: " + latitude + "," + longitude);
                return "Unknown Location";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error retrieving address";
        }
    }


}
