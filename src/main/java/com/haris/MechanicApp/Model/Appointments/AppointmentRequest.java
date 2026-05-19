package com.haris.MechanicApp.Model.Appointments;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "appointment_requests")
@Data
public class AppointmentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================
    // APPOINTMENT RELATION
    // =========================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "appointment_id",
            referencedColumnName = "app_id",
            nullable = false
    )
    private Appointments appointment;

    // =========================
    // MECHANIC RELATION
    // =========================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "mechanic_id",
            referencedColumnName = "id",
            nullable = false
    )
    private Mechanic mechanic;

    // =========================
    // REQUEST STATUS
    // =========================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    // =========================
    // REJECTION REASON
    // =========================
    @Column(columnDefinition = "TEXT")
    private String reason;

    // =========================
    // REQUEST CREATED TIME
    // =========================
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    // =========================
    // ACCEPT / REJECT TIME
    // =========================
    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "accepted_by_mechanic_id")
    private Long acceptedByMechanicId;

    @Column(name = "reminder_sent")
    private boolean reminderSent = false;

}