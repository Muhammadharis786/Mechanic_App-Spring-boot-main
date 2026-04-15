package com.haris.MechanicApp.Model.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestUserDto {
    private Long userid;
    private String username;
    private  double distance ;
    private String userlocname;
    private double lat;
    private  double lon;
    private String eta;
     private double price;
    private String userimage;
}
