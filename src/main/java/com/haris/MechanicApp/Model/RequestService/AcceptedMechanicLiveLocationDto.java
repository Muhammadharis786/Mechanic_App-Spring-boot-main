package com.haris.MechanicApp.Model.RequestService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AcceptedMechanicLiveLocationDto {
    private Long requestId;
    private Long mechanicId;
    private Double latitude;
    private Double longitude;
    private Double bearing;
    private Double speed;
    private Double distance;
    private String eta;
}