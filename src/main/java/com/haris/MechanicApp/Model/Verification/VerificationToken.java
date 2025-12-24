package com.haris.MechanicApp.Model.Verification;


import jakarta.persistence.*;

import java.util.Calendar;
import java.util.Date;

@Entity
@Table(name="verificationtoken")
public class VerificationToken  {
   @Id
   @GeneratedValue (strategy = GenerationType.IDENTITY)

   private long id ;
    private Date expiryDate ;
   private String token;
    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "userid")
    private User user;
   public  VerificationToken() {

    }
    public VerificationToken(User user, String token) {
        this.user = user;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
