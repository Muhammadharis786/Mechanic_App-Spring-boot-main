package com.haris.MechanicApp.Model.RequestService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateServiceRequestDto {
    private String serviceType;
    private String userNotes;

    private Double userLatitude;
    private Double userLongitude;

    private String locationName;
}
