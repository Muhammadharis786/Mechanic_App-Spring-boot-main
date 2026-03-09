package com.haris.MechanicApp.Model.Mechanic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MechOtpDto {
    private long userid;
    private String phonenumber;
    private String password;

}
