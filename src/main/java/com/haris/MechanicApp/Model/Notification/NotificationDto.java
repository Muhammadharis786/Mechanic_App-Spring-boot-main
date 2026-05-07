package com.haris.MechanicApp.Model.Notification;

import com.haris.MechanicApp.Model.Appointments.Appointments;
import com.haris.MechanicApp.Model.Appointments.AutoAppointmentDto;
import com.haris.MechanicApp.Model.Appointments.ManualAppointmentDto;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Mechanic.MechanicDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.A;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
    private Long id;
    private String title;
    private String message;
    private String type;
    private boolean isRead;
    private LocalDateTime createdAt;
    private MechanicDTO mechanic;
    private AutoAppointmentDto autoAppointment;
    private ManualAppointmentDto  manualAppointment;
}