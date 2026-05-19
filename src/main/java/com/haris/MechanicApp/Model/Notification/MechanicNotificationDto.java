package com.haris.MechanicApp.Model.Notification;

import com.haris.MechanicApp.Model.Appointments.AutoAppointmentDto;
import com.haris.MechanicApp.Model.Mechanic.MechanicDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MechanicNotificationDto {
    private Long notificationId;
    private String appointmentId;
    private NotificationType type;
    private String message;
    private boolean isRead;
    private Instant createdAt;



}