package com.haris.MechanicApp.Repository;

import com.haris.MechanicApp.Model.Appointments.AppointmentRequest;
import com.haris.MechanicApp.Model.Appointments.Appointments;
import com.haris.MechanicApp.Model.Appointments.RequestStatus;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppointmentRequestRepository extends JpaRepository<AppointmentRequest,Long> {


    Optional<AppointmentRequest> findByMechanicAndAppointment(Mechanic mechanic, Appointments appointment);

    List<AppointmentRequest> findByAppointment(Appointments appointment);


    List<AppointmentRequest> findByStatus(RequestStatus requestStatus);

    List<AppointmentRequest> findByStatusAndReminderSentFalse(RequestStatus requestStatus);

    Optional<AppointmentRequest> findByMechanicAndAppointment_AppointmentId(Mechanic mechanic, String appointmentid);

    List<AppointmentRequest> findByMechanic(Mechanic mechanic);
}
