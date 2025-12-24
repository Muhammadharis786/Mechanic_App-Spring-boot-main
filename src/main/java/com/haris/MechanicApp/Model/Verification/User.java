package com.haris.MechanicApp.Model.Verification;

import jakarta.persistence.*;

import java.math.BigDecimal;


@Table(name = "users")
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long userid ;

    private String password;
    private String email ;
    private String username;
    @Column(name = "last_latitude", precision = 10, scale = 8)
    private BigDecimal lastLatitude;
    @Column(name = "last_longitude", precision = 11, scale = 8)
    private BigDecimal lastLongitude;
    @Column(name = "image_url")
    private String userimgurl;

    public BigDecimal getLastLongitude() {
        return lastLongitude;
    }

    public void setLastLongitude(BigDecimal lastLongitude) {
        this.lastLongitude = lastLongitude;
    }

    public BigDecimal getLastLatitude() {
        return lastLatitude;
    }

    public void setLastLatitude(BigDecimal lastLatitude) {
        this.lastLatitude = lastLatitude;
    }

    public String getUserimgurl() {
        return userimgurl;
    }

    public void setUserimgurl(String userimgurl) {
        this.userimgurl = userimgurl;
    }

    @Column(name = "enable")
    private boolean enabled=false;

    @Column(name = "registration_date")
    private String registrationDate;

    private String role= "User";

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public User(String password, String email ) {


        this.password = password;
        this.email = email;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public User() {

    }

    public long getId() {
        return userid;
    }

    public void setId(long id) {
        this.userid = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
