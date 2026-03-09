package com.haris.MechanicApp.Model.Verification;

import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

@Entity
@Table(name = "verificationtokenmechanic") // Lowercase, jaisa aapne DDL mein likha hai

public class VerificationTokenMechanic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private long id ;
    private Date expiryDate ;
    private String token;
    @OneToOne(targetEntity = Mechanic.class, fetch = FetchType.EAGER)
    @JoinColumn(name = "mechid", referencedColumnName = "id", nullable = false, unique = true)
    private Mechanic mechanic;


    public  VerificationTokenMechanic() {

    }
    public VerificationTokenMechanic(Mechanic mechanic, String token) {
        this.mechanic = mechanic;
        this.token = token;
    }

    public void setExpiryDate(int ExpiryDate) {
        this.expiryDate = calculateExpiryDate(ExpiryDate);
    }



    public Date getExpiryDate() {
        return expiryDate;
    }

    private Date calculateExpiryDate(int expiryTimeInMinutes) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(cal.getTime().getTime()));
        cal.add(Calendar.MINUTE, expiryTimeInMinutes);
        return new Date(cal.getTime().getTime());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }



    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Mechanic getMechanic() {
        return mechanic;
    }

    public void setMechanic(Mechanic mechanic) {
        this.mechanic = mechanic;
    }

}
