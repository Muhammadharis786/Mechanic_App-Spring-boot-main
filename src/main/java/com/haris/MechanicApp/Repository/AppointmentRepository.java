package com.haris.MechanicApp.Repository;

import com.haris.MechanicApp.Model.Appointments.AppointmentStatus;
import com.haris.MechanicApp.Model.Appointments.Appointments;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Verification.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository  extends JpaRepository<Appointments , String> {

    List<Appointments> findByUser(User user);

    // ye query jb chaligi jab user ki request pending hay 1 din tk aur wo status pending say expired krdegi after 1 day
    @Modifying
    @Query("UPDATE Appointments a SET a.status = 'EXPIRED' WHERE a.status = 'PENDING' AND a.createdAt < :cutoff")
    void expirePendingAppointments(Instant cutoff);

    List<Appointments> findByMechanic(Mechanic mechanic);

// AppointmentRepository.java

    @Query("SELECT a FROM Appointments a WHERE a.mechanic = :mechanic " +
            "AND a.status = 'COMPLETED' " +
            "AND a.completedAt >= :startOfDay " +
            "AND a.completedAt < :endOfDay")
    List<Appointments> findTodayCompletedByMechanic(
            @Param("mechanic") Mechanic mechanic,
            @Param("startOfDay") Instant startOfDay,
            @Param("endOfDay") Instant endOfDay
    );

    Optional<Appointments> findByAppointmentIdAndUser(String appointmentid, User user);

    Optional<Appointments> findByAppointmentId(String appointmentid);

    Optional<Appointments> findByMechanicAndAppointmentId(Mechanic mechanic, String appointmentid);

    List<Appointments> findTop5ByMechanicAndStatusOrderByCompletedAtDesc(
            Mechanic mechanic, AppointmentStatus status
    );

    List<Appointments> findByMechanicAndStatusOrderByCompletedAtDesc(Mechanic mech, AppointmentStatus appointmentStatus);


    // 1. Active Appointments count karne ke liye (ACCEPTED se COMPLETED se pehle wali statuses)
    @Query("SELECT COUNT(a) FROM Appointments a WHERE a.mechanic = :mechanic " +
            "AND a.status IN ('ACCEPTED', 'ON_THE_WAY', 'ARRIVED', 'IN_PROGRESS', 'WORK_COMPLETED', 'PAYMENT_PROCESS')")
    long countActiveByMechanic(@Param("mechanic") Mechanic mechanic);

    // 2. Aaj ki active appointments (due today) — jinki appointmentDate aaj ki ho aur status active ho
    @Query("SELECT COUNT(a) FROM Appointments a WHERE a.mechanic = :mechanic " +
            "AND a.status IN ('ACCEPTED', 'ON_THE_WAY', 'ARRIVED', 'IN_PROGRESS', 'WORK_COMPLETED', 'PAYMENT_PROCESS') " +
            "AND a.appointmentDate = :today")
    long countDueTodayByMechanic(@Param("mechanic") Mechanic mechanic, @Param("today") LocalDate today);

    // 3. This month aur last month jobs count (for +12% logic)
    @Query("SELECT COUNT(a) FROM Appointments a WHERE a.mechanic = :mechanic " +
            "AND a.createdAt >= :start AND a.createdAt < :end")
    long countByMechanicAndCreatedAtBetween(@Param("mechanic") Mechanic mechanic,
                                            @Param("start") Instant start,
                                            @Param("end") Instant end);

    // 4. Monthly earnings filter ke liye
    @Query("SELECT a FROM Appointments a WHERE a.mechanic = :mechanic " +
            "AND a.status = 'COMPLETED' " +
            "AND a.completedAt >= :start AND a.completedAt < :end")
    List<Appointments> findCompletedByMechanicAndMonth(@Param("mechanic") Mechanic mechanic,
                                                       @Param("start") Instant start,
                                                       @Param("end") Instant end);


    List<Appointments> findByStatus(AppointmentStatus status);
    List<Appointments> findAll();  // already hai JpaRepository mein
}
