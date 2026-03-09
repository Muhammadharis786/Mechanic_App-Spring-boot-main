package com.haris.MechanicApp.Model.Verification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MechanicToken {
    private long id;
    private String token;
    private String phonenumber;
}
