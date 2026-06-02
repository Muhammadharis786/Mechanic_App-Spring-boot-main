package com.haris.MechanicApp.Model.Review;


import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.RequestService.RequestService;
import com.haris.MechanicApp.Model.Verification.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer rating;

    private String comment;

    @Column (name = "created_at")
    private Instant createdAt = Instant.now();

    @ManyToOne
    private User user;

    @ManyToOne
    private Mechanic mechanic;



    // 🔥 Generic linking
    @Enumerated(EnumType.STRING)
    @Column (name = "service_type")
    private ServiceType serviceType;

    // "EMERGENCY" or "APPOINTMENT"
     @Column (name = "service_id")
    private Long serviceId;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
