package com.haris.MechanicApp.Model.Mechanic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NearbyMechanicLocationDto {
    private String mapSessionId;
    private Long mechanicId;
    private Double latitude;
    private Double longitude;
    private Double bearing;
    private Double speed;
}