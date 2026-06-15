package com.haris.MechanicApp.Model.Mechanic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyServicesDTO {

    private int totalCompleted;
    private int totalCancelled;
    private int totaljobsCompleted;
    private int totalEarning;
    private BigDecimal averageRating;
    private int totalReviews;
    private double completionRate; // % mein

    // ✅ NAYE FIELDS ADD KARO:
    private int activeJobs;         // ACCEPTED se pehle COMPLETED tak
    private int dueToday;           // Aaj ki date wali active appointments
    private double growthPercent;   // +12% this month
    private double lastMonthEarning; // Performance overview ke liye month filter

}