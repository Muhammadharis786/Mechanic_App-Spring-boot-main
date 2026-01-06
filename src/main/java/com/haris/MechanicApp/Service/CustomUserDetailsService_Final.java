package com.haris.MechanicApp.Service;

import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Verification.User;
import com.haris.MechanicApp.Repository.MechanicRepository;
import com.haris.MechanicApp.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Primary
public class CustomUserDetailsService_Final   implements UserDetailsService {
    @Autowired
    private MechanicRepository mechanicRepository;
    @Autowired
    private UserRepository userRepository;



    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {

        // 1. Check if the identifier is an email (contains '@')
        if (identifier.contains("@")) {
            // User login with Email
            Optional<User> userOpt =  userRepository.findByEmail(identifier);
            if (userOpt.isPresent()) {
                return new MyPrincipal(userOpt.get());
            }
            throw new UsernameNotFoundException("User not found with email: " + identifier);
        } else {
            // Mechanic login with Phone Number
            Optional<Mechanic> mechanicOpt = mechanicRepository.findByPhonenumber(identifier);
            if (mechanicOpt.isPresent()) {
                return new MyPrincipalMechanic(mechanicOpt.get());
            }
            throw new UsernameNotFoundException("Mechanic not found with phone: " + identifier);
        }
    }
}


