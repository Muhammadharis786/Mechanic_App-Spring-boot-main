package com.haris.MechanicApp.Model.Verification;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long userid;

    private String password;
    private String email;
    private String username;
    @Column(name = "last_latitude", precision = 10, scale = 8)
    private BigDecimal lastLatitude;
    @Column(name = "last_longitude", precision = 11, scale = 8)
    private BigDecimal lastLongitude;
    @Column(name = "image_url")
    private String userimgurl;

    @Column(name = "enable")
    private boolean enabled = false;


    @Column(name = "phonenumber")
    private String phonenumber;

    @Column(name = "registration_date")
    private String registrationDate;

    // This part is changed to support multiple roles
    @ElementCollection(targetClass = Role.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "userid"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<Role> roles = new HashSet<>();
}


