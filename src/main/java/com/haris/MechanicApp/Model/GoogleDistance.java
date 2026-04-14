package com.haris.MechanicApp.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;


public class GoogleDistance {



    private static final String API_KEY = "AIzaSyBpyZg2i30gOLUKK0furYdGDbWXe4lqpkU";






// Baqi aapke purane imports...

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
                    .queryParam("departure_time", "now")
                    .queryParam("key", API_KEY)
                    .build()
                    .toUri(); // <= String ke bajaye URI banaya gaya hai


            RestTemplate restTemplate = new RestTemplate();

            // FIX: String URL ki jaga URI object pass kiya hai
            String response = restTemplate.getForObject(uri, String.class);


            JSONObject json = new JSONObject(response);

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

    public String getAddressFromLatLng(BigDecimal mechLatitude, BigDecimal mechLongitude) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng="
                    + mechLatitude + "," + mechLongitude
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
                System.out.println("No address found for: " + mechLatitude + "," + mechLongitude);
                return "Unknown Location";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error retrieving address";
        }
    }


}
