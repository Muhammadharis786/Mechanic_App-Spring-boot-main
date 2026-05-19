package com.haris.MechanicApp.Repository;

import com.haris.MechanicApp.Model.Appointments.Appointments;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Verification.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
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



    Optional<Appointments> findByAppointmentIdAndUser(String appointmentid, User user);

    Optional<Appointments> findByAppointmentId(String appointmentid);

    Optional<Appointments> findByMechanicAndAppointmentId(Mechanic mechanic, String appointmentid);
}
