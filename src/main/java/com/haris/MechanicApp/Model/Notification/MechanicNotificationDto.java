package com.haris.MechanicApp.Model.Notification;

import com.haris.MechanicApp.Model.Appointments.AutoAppointmentDto;
import com.haris.MechanicApp.Model.Mechanic.MechanicDTO;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class MechanicNotificationDto {
    private Long notificationId;
    private Long mechanicId;
    private Long appointmentId;
    private String type;
    private String title;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;
    private AutoAppointmentDto  autoAppointmentDto;
    private MechanicDTO mechanicDTO;

}