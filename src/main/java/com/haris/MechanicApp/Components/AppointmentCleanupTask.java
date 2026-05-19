package com.haris.MechanicApp.Components;

import com.haris.MechanicApp.Model.Appointments.AppointmentRequest;
import com.haris.MechanicApp.Model.Appointments.AppointmentResponseDTO;
import com.haris.MechanicApp.Model.Appointments.Appointments;
import com.haris.MechanicApp.Model.Appointments.RequestStatus;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Notification.Notification;
import com.haris.MechanicApp.Model.Notification.NotificationType;
import com.haris.MechanicApp.Repository.AppointmentRepository;
import com.haris.MechanicApp.Repository.AppointmentRequestRepository;
import com.haris.MechanicApp.Repository.MechanicNotificationRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class AppointmentCleanupTask {

    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    private AppointmentRequestRepository appointmentRequestRepository;

    @Autowired
    MechanicNotificationRepository notificationRepository;
// ye ek auto schedula hay jo kay har ghantay may run hoga automatically
    //ye is lie lgya hay tkay pending request ko 1 din say zyada ko wo pending sa expired krday

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireOldAppointments() {
        // Instant.now() hamesha UTC time dega
        Instant cutoff = Instant.now().minus(1, ChronoUnit.DAYS);
        appointmentRepository.expirePendingAppointments(cutoff);
    }
    @Transactional

    @Scheduled(fixedRate = 60000)
    public void sendAppointmentReminders() {

        List<AppointmentRequest> acceptedRequests =
                appointmentRequestRepository
                        .findByStatusAndReminderSentFalse(RequestStatus.ACCEPTED);

        Instant now = Instant.now();

        for (AppointmentRequest request : acceptedRequests) {

            Appointments appointment = request.getAppointment();
            Mechanic mechanic = request.getMechanic();

            if (appointment == null || mechanic == null) continue;

            LocalDateTime appointmentDateTime =
                    LocalDateTime.of(
                            appointment.getAppointmentDate(),
                            appointment.getAppointmentTime()
                    );

            Instant appointmentInstant =
                    appointmentDateTime.atZone(ZoneId.systemDefault()).toInstant();

            Instant reminderTime = appointmentInstant.minus(Duration.ofMinutes(10));

            // ✅ safe 1-minute window
            if (!now.isBefore(reminderTime)
                    && now.isBefore(reminderTime.plusSeconds(60))) {

                Notification notification = new Notification();
                notification.setAppointments(appointment);
                notification.setMechanic(mechanic);
                notification.setUser(appointment.getUser());
                notification.setTitle("Appointment Reminder");
                notification.setMessage("Your appointment starts in 10 minutes. Get ready!");
                notification.setType(NotificationType.APPOINTMENT_REMINDER);
                notification.setCreatedAt(Instant.now());

                notificationRepository.save(notification);

                AppointmentResponseDTO dto = new AppointmentResponseDTO();
                dto.setTitle("Appointment Reminder");
                dto.setMessage("Your appointment starts in 10 minutes. Get ready!");
                dto.setImage(mechanic.getMechanicimgurl());
                dto.setCreatedAt(Instant.now());

                simpMessagingTemplate.convertAndSend(
                        "/topic/reminder/mechanic/" + mechanic.getId(),
                        dto
                );

                request.setReminderSent(true);
                appointmentRequestRepository.save(request);
            }
        }
    }
}