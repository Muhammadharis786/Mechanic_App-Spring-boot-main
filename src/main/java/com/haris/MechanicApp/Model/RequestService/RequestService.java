package com.haris.MechanicApp.Model.RequestService;

import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Verification.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;
    // ----- ACTUAL FOREIGN KEYS YAHAN HAIN -----

    // Kis user ne request ki
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "userid", nullable = false) // user table se link karega
    private User user;
    // Kis mechanic ko request mili / kisnay accept ki
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", referencedColumnName = "id") // mechanic table se link karega (Nullable)
    private Mechanic mechanic;
    // -------------------------------------------
    private Double userLatitude;
    private Double userLongitude;
    private String locationName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceRequestStatus requestStatus = ServiceRequestStatus.PENDING;
    private String serviceType;

    @Column(columnDefinition = "TEXT")
    private String userNotes;
    // Pricing & ETA
    private String estimatedTimeOfArrival;
    @Column(name = "inspection_price")
    private Double inspectionPrice;

    @Column(name = "visiting_charges")
    private Double visitingcharges;

    private Double finalAmount;
    private String paymentStatus = "UNPAID";
    @Column(updatable = false)
    private Instant createdAt;
    private Instant completedAt;


    @Column(nullable = false , name = "is_fixed_charge_accepted")
    private Boolean isFixedChargeAccepted = false;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

}