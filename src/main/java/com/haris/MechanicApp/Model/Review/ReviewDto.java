package com.haris.MechanicApp.Model.Review;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewDto {

    private Long serviceId;
    private String serviceType;
    private Integer rating;
    private String comment;

    // getters setters
}