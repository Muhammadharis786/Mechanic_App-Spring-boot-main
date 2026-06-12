package com.haris.MechanicApp.Model.KYC;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KycResult {
    private boolean verified;
    private float similarityScore;
    private String message;
}