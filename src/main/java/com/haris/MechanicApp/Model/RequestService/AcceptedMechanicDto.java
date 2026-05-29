package com.haris.MechanicApp.Model.RequestService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AcceptedMechanicDto {
    private Long requestId;
    private Long mechanicId;
    private String mechanicName;
    private String mechanicNumber;
    private String mechanicImage;
    private BigDecimal mechanicRating;
    private int mechanicTotalReviews;
    private String mechanicShopName;
    private String mechanicType;
    private int mechanicExperience;
    private Double mechanicLatitude;
    private Double mechanicLongitude;
    private Double distance;
    private String eta;
}
