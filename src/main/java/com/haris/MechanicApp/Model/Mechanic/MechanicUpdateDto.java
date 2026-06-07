package com.haris.MechanicApp.Model.Mechanic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MechanicUpdateDto {
    private String name ;
    private String shopaddress ;
    private int experience;

    private String mechurl ;

}
