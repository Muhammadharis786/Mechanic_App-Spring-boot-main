package com.haris.MechanicApp.Model.Location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationDTO {
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String serviceType;
}
