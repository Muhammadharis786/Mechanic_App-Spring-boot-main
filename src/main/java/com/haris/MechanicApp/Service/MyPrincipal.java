package com.haris.MechanicApp.Service;


import com.haris.MechanicApp.Model.Verification.User;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class MyPrincipal implements UserDetails {

    private final   User user;
    public MyPrincipal(User user) {
        System.out.println("\n--- MyPrincipal: CONSTRUCTOR CALLED ---");
        if (user == null) {
            System.out.println("ERROR: User object passed to MyPrincipal is NULL!");
            this.user = new User(); // Avoid NullPointerException
        } else {
            System.out.println("User object received. Phonenumber: " + user.getPhonenumber());
            this.user = user;
        }
        // --- DEBUGGING END ---
    }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return  user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                    .collect(Collectors.toList());

        }

    @Override
    public @Nullable String getPassword() {

        // --- DEBUGGING ---
        // Password ko print na karein, lekin yeh check karein ke null to nahi.
        String password = user.getPassword();
        System.out.println("MyPrincipal getPassword(): " + (password == null ? "NULL" : "Password is present (hash)"));
        return password;    }

    @Override
    public String getUsername() {

        // --- DEBUGGING ---
        String username = user.getPhonenumber();
        System.out.println("MyPrincipal getUsername(): Returning '" + username + "'");
        return username;    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }
    public String getUserName() {
        return user.getUsername(); // your User entity's username field
    }
    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }




}
