package com.haris.MechanicApp.Model.Appointments;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MechanicAppointmentDTO {
    private String appointmentid ;
    private String serviceType;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String problemDescription;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private Integer visitingcharges;
    private Instant created_at ;
    private AppointmentStatus status;

    private String useraddress;
    private String userimage ;
    private String username;
    private String userphonenumber;

    private BigDecimal mechshoplat;
    private BigDecimal mechshoplong;


}
