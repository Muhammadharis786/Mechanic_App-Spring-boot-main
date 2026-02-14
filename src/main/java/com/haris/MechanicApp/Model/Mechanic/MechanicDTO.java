package com.haris.MechanicApp.Model.Mechanic;


import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MechanicDTO {
    private String name;
    private String MechanicType;
    private BigDecimal averagerating;
    private int experience;
    private boolean isactive ;
    private BigDecimal distance;
    private String phonenumber;
    private String mechanicimgurl;
    private String mechaniclocname;
    private boolean isengaged;
    private BigDecimal latitude;
    private BigDecimal longitude;

}
