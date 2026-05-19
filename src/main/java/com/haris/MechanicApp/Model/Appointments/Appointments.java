package com.haris.MechanicApp.Model.Appointments;

import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Verification.User;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data
public class Appointments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    @Column(name = "app_id")
    private String appointmentId;
      @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(name = "problem_description", columnDefinition = "TEXT")
    private String problemDescription;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    private LocalDate appointmentDate;
    private LocalTime appointmentTime;

    // Enum mapping yahan hai
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.PENDING;
    @Column(precision = 10, scale = 8 , name = "latitude")
    private  BigDecimal latitude;
    @Column(precision = 10, scale = 8 , name = "longitude")

    private  BigDecimal longitude;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "userid", nullable = false) // user table se link karega
    private User user;

    @Column(name = "repair_amount")
    private   Integer  repairAmount=0;

    @Column(name = "visiting_charge")
    private Integer visitingCharge = 300 ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", referencedColumnName = "id") // mechanic table se link karega (Nullable)
    private Mechanic mechanic;

    @Column(name = "reason")
    private String reason;



}
