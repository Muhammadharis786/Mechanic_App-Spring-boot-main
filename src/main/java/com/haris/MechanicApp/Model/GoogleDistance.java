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






// Baqi aapke purane imports...
    public List<RoadInfo> openmatrixdistance (double userLatitude, double userLongitude, List<org.springframework.data.geo.Point> mechanicCordinates){
    List<RoadInfo> roadInfos = new ArrayList<>();
        System.out.println("API Key = " + apiKey);
     String MATRIX_URL = "https://api.openrouteservice.org/v2/matrix/driving-car";
        if (mechanicCordinates == null || mechanicCordinates.isEmpty()) {
            return roadInfos; // empty list, crash nahi hoga
        }

        try {
            RestTemplate restTemplate = new RestTemplate();

            // ORS locations format: [longitude, latitude] — Google se ulta order hai
            JSONArray locationsArray = new JSONArray();
            locationsArray.put(new JSONArray().put(userLongitude).put(userLatitude)); // index 0 = origin

            for (Point point : mechanicCordinates) {

                locationsArray.put(new JSONArray().put(point.getX()).put(point.getY()));
            }

            JSONArray destinationsIndexArray = new JSONArray();
            for (int i = 1; i <= mechanicCordinates.size(); i++) {
                destinationsIndexArray.put(i);
            }

            JSONObject body = new JSONObject();
            body.put("locations", locationsArray);
            body.put("sources", new JSONArray().put(0));
            body.put("destinations", destinationsIndexArray);
            body.put("metrics", new JSONArray().put("distance").put("duration"));
            body.put("units", "km");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(MATRIX_URL, request, String.class);

            JSONObject json = new JSONObject(response.getBody());

            System.out.println("This is json object: "+ json);
            JSONArray distancesRows = json.optJSONArray("distances");
            JSONArray durationsRows = json.optJSONArray("durations");

            if (distancesRows == null || distancesRows.length() == 0) {
                System.out.println("ORS returned no distance rows: " + response.getBody());
                return roadInfos;
            }

            JSONArray distances = distancesRows.getJSONArray(0);
            JSONArray durations = durationsRows.getJSONArray(0);

            for (int i = 0; i < distances.length(); i++) {
                if (distances.isNull(i)) {
                    roadInfos.add(new RoadInfo(-1, "NULL")); // unreachable location
                    continue;
                }
                double distanceKm = distances.getDouble(i);
                double durationSec = durations.getDouble(i);
                String eta = Math.round(durationSec / 60) + " mins";
                roadInfos.add(new RoadInfo(distanceKm, eta));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>(); // fail-safe
        }
//for (RoadInfo info :  roadInfos) {
//    System.out.println("Distance: "+ info.getDistance() +"Time: " + info.getDistancetime());
//}
        return roadInfos;
//     for (Point points :  mechanicCordinates) {
//         System.out.println("This is first mechanic cordinates:"+ points.getX()+" " +  points.getY());
//     }
//     return null;
    }





    public List<RoadInfo> getBatchRoadDistances(double userLatitude, double userLongitude, String destinationsParam) {
        List<RoadInfo> roadInfos = new ArrayList<>();


        if (destinationsParam == null || destinationsParam.isEmpty()) {
            return roadInfos;
        }

        try {
            String originLocation = userLatitude + "," + userLongitude;

            // FIX: Use .build().toUri() instead of .toUriString()
            // Is se RestTemplate String encode nahi karega balkay direct URI read karega (Double Encoding Bug fixed)
            URI uri = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host("maps.googleapis.com")
                    .path("/maps/api/distancematrix/json")
                    .queryParam("origins", originLocation)
                    .queryParam("destinations", destinationsParam)
                    .queryParam("key", API_KEY)
                    .build()
                    .toUri(); // <= String ke bajaye URI banaya gaya hai


            RestTemplate restTemplate = new RestTemplate();

            // FIX: String URL ki jaga URI object pass kiya hai
            String response = restTemplate.getForObject(uri, String.class);


            JSONObject json = new JSONObject(response);
            System.out.println("This is json object return from google distance api"+ json);

            // Google returns single "row" for our single origin
            JSONArray elementsArray = json.getJSONArray("rows")
                    .getJSONObject(0)
                    .getJSONArray("elements");

            // Loop over elements
            for (int i = 0; i < elementsArray.length(); i++) {
                JSONObject element = elementsArray.getJSONObject(i);

                if (element.getString("status").equals("OK")) {
                    int distanceMeters = element.getJSONObject("distance").getInt("value");
                    String eta = element.getJSONObject("duration").getString("text");
                    double distanceKm = (double) distanceMeters / 1000.0;
                    roadInfos.add( new RoadInfo(distanceKm, eta));


                } else {
                    roadInfos.add( new RoadInfo(-1, "NULL"));
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
