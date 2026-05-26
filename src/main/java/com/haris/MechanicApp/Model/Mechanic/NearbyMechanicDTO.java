package com.haris.MechanicApp.Model.Mechanic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NearbyMechanicDTO {
    private Long mechanicId;

    private Double latitude;
    private Double longitude;
}
