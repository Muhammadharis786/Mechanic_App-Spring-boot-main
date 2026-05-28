package com.haris.MechanicApp.Model.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LiveLocationDto {
    private Long senderId;     // User ID ya Mechanic ID jo location bhej raha hai
    private String senderType; // "user" ya "mechanic"
    private double latitude;
    private double longitude;
}
