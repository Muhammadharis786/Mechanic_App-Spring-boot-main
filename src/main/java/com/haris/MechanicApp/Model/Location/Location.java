package com.haris.MechanicApp.Model.Location;

import com.haris.MechanicApp.Model.Verification.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class Location {


    private BigDecimal latitude;
    private BigDecimal longitude;



}
