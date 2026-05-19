package com.haris.MechanicApp.Model.Appointments;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentResponseDTO {
    private String image;
    private String message;
    private String title;
    private String type;
    private Instant createdAt;
}
