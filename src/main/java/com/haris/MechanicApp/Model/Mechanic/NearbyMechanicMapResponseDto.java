package com.haris.MechanicApp.Model.Mechanic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NearbyMechanicMapResponseDto {
    private String mapSessionId;
    private List<NearbyMechanicDTO> mechanics;
}