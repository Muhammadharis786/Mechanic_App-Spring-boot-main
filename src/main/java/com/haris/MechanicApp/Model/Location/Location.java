package com.haris.MechanicApp.Model.Location;

import com.haris.MechanicApp.Model.Verification.User;
import jakarta.persistence.*;

import java.math.BigDecimal;


public class Location {



    private BigDecimal latitude;
    private BigDecimal longitude;


    public Location() {

    }




    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }
}
