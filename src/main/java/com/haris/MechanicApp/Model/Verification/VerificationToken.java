package com.haris.MechanicApp.Model.Verification;


import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

@Entity
@Table(name="verificationtoken")
public class VerificationToken  {
   @Id
   @GeneratedValue (strategy = GenerationType.IDENTITY)

   private long id ;
    private Instant expiryDate ;
   private String token;
    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "userid")
    private User user;


    private LocalDateTime createdDate;

    public  VerificationToken() {

    }
    public VerificationToken(User user, String token) {
        this.user = user;
        this.token = token;
    }

    public void setExpiryDate(int expiryTimeInMinutes) {
        this.expiryDate = Instant.now().plus(expiryTimeInMinutes, ChronoUnit.MINUTES);
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getExpiryDate() {
        return expiryDate;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
