package com.haris.MechanicApp.Service;

import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MyPrincipalMechanic implements UserDetails {

    private final Mechanic mechanic;
    public MyPrincipalMechanic(Mechanic mechanic) {
        this.mechanic = mechanic;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_MECHANIC"));
    }

    @Override
    public @Nullable String getPassword() {
        return mechanic.getPassword();
    }

    @Override
    public String getUsername() {

          return  mechanic.getPhonenumber();

    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
