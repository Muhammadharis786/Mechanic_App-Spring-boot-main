package com.haris.MechanicApp.Components;

import com.haris.MechanicApp.Repository.AppointmentRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

import java.time.temporal.ChronoUnit;

@Component
public class AppointmentCleanupTask {

    @Autowired
    private AppointmentRepository appointmentRepository;
// ye ek auto schedula hay jo kay har ghantay may run hoga automatically
    //ye is lie lgya hay tkay pending request ko 1 din say zyada ko wo pending sa expired krday

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireOldAppointments() {
        // Instant.now() hamesha UTC time dega
        Instant cutoff = Instant.now().minus(1, ChronoUnit.DAYS);
        appointmentRepository.expirePendingAppointments(cutoff);
    }
}