package com.haris.MechanicApp.Model;

import org.json.JSONObject;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;

public class GoogleDistance {



    private static final String API_KEY = "AIzaSyBpyZg2i30gOLUKK0furYdGDbWXe4lqpkU";

    public   float CalulateDistance (BigDecimal mechlatitude, BigDecimal mechlongitude ,
                            BigDecimal userlatitude , BigDecimal userlongitude ){



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
        System.out.println("Distance: " + distanceText + " (" + distanceinkm + " meters)");

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
