package com.haris.MechanicApp.Model.Mechanic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mechanic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(name = "mecahnic_id")
    private String mechanicid;
    private String password;





    // 📱 Contact Information
    @Column(name = "phone_number")
    private String phonenumber;


    // 📍 Location & Address
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;
     @Column(name = "shop_address")
    private String shopaddress;           // Workshop/shop address


    // 📄 Documents & Verification
    private String mechanicimgurl;
    private String cnicfronturl;
    private String cnicbackurl;
    @Column(name = "cnic_number")
    private String cnicNumber;
     @Column(name = "is_verified")
    private boolean isverified = false;

    // 🔧 Work Details
    private String mechanictype;

      @Column(name = "experience_years")
    private int experienceyears;
       // Minimum charge

    // 📊 Availability & Status
     @Column(name = "is_engaged")
    private boolean isengaged = false;

    @Column(name = "is_active")
    private boolean isactive = true;

         @Column(name = "working_hours")         // Account active/deactivated
    private String workinghours;          // e.g., "9 AM - 6 PM"

    // 📈 Statistics & Performance
    @Column(name = "last_seen")
    private Timestamp lastSeen;

    @Column(name = "total_jobs_completed")
    private int totalJobsCompleted = 0;

    @Column(name = "total_jobs_cancelled")
    private int totalJobsCancelled = 0;


    @Column(name = "average_rating", precision = 3 ,scale = 2)
    private BigDecimal averageRating = new BigDecimal("5.0");

    @Column(name = "total_reviews")
    private int totalReviews = 0;

    private  int totalearning;


}
