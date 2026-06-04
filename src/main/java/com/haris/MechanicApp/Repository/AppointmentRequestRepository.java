package com.haris.MechanicApp.Repository;

import com.haris.MechanicApp.Model.Appointments.AppointmentRequest;
import com.haris.MechanicApp.Model.Appointments.Appointments;
import com.haris.MechanicApp.Model.Appointments.RequestStatus;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AppointmentRequestRepository extends JpaRepository<AppointmentRequest,Long> {


    Optional<AppointmentRequest> findByMechanicAndAppointment(Mechanic mechanic, Appointments appointment);

    List<AppointmentRequest> findByAppointment(Appointments appointment);


    List<AppointmentRequest> findByStatus(RequestStatus requestStatus);

    List<AppointmentRequest> findByStatusAndReminderSentFalse(RequestStatus requestStatus);

    Optional<AppointmentRequest> findByMechanicAndAppointment_AppointmentId(Mechanic mechanic, String appointmentid);

    List<AppointmentRequest> findByMechanic(Mechanic mechanic);

    // ye query jb chaligi jab user ki request pending hay 1 din tk aur wo status pending say expired krdegi after 1 day
    @Modifying
    @Query("UPDATE AppointmentRequest a SET a.status = 'EXPIRED' WHERE a.status = 'PENDING' AND a.createdAt < :cutoff")
    void expirePendingAppointments(Instant cutoff);

    List<AppointmentRequest> findByMechanicAndStatus(Mechanic mechanic,
                                                     RequestStatus requestStatus);
}
