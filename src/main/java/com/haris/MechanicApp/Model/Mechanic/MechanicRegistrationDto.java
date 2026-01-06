package com.haris.MechanicApp.Model.Mechanic;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MechanicRegistrationDto {
    private long userid;
    private String name;
    private String phonenumber;
    private String password;
    private String shopaddress;
    private String mechanictype;
    private int experienceyears;
    private String workinghours;
    private String mechanicimgurl;
    private String cnicfronturl;
    private String cnicbackurl;
    private boolean otpVerified;
}
