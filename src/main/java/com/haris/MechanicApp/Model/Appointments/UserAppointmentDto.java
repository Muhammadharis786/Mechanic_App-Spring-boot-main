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

public class UserAppointmentDto {

    private String appointmentid ;
    private String serviceType;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String problemDescription;
    private String address;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private Integer visitingcharges;
    private Instant created_at ;



    private long mechanicid;
    private BigDecimal mechshoplat ;
    private BigDecimal mechshoplong;
    private String mechname ;
    private String mechimage ;
    private BigDecimal mechrating ;
    private int mechexperience ;
    private String mechtype;
    private String mechnumber;
    private int totalreviews ;
    private String mechanicshopaddress;
    private AppointmentStatus status;
}
