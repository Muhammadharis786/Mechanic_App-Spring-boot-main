package com.haris.MechanicApp.Model.Notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserNotificationDto {
    private long notificationid;
    private String appointmentid ;
    private NotificationType type ;
    private String message ;
    private Instant created_at;

    private boolean isread ;
}
