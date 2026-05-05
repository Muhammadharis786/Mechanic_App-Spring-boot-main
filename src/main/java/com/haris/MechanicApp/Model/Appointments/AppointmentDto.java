package com.haris.MechanicApp.Model.Appointments;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDto {

   private String serviceType;
   private BigDecimal latitude;
   private BigDecimal longitude;
    private String problemDescription;
    private String address;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;

}
