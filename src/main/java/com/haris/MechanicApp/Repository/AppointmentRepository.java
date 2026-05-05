package com.haris.MechanicApp.Repository;

import com.haris.MechanicApp.Model.Appointments.Appointments;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository  extends JpaRepository<Appointments , Long> {

}
