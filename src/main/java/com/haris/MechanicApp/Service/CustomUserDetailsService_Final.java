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


        // --- DEBUGGING START ---
        System.out.println("\n--- CustomUserDetailsService: loadUserByUsername CALLED ---");
        System.out.println("Attempting to load user with identifier: '" + identifier + "'");
        // --- DEBUGGING END ---

      String [] parts =  identifier.split(";");
      if(parts.length != 2){
          System.out.println("ERROR: Identifier format is invalid. Throwing UsernameNotFoundException.");

          throw new UsernameNotFoundException("Invalid Login Format");
      }
      String phonenumber= parts[0];
      String Role = parts[1];
      if("USER".equalsIgnoreCase(Role)){
          System.out.println("Searching for a USER in the database...");

          Optional<User> checkuser =  userRepository.findByPhonenumber(phonenumber);
          if(checkuser.isPresent()){
              System.out.println("SUCCESS: User found! Creating MyPrincipal object.");

              return  new MyPrincipal(checkuser.get());
          }
          System.out.println("FAILURE: User not found with Phone Number: " + phonenumber);

          throw new UsernameNotFoundException("User not found with Phone Number: " + phonenumber);

      }
      else if ("MECHANIC".equalsIgnoreCase(Role)){
          System.out.println("Searching for a MECHANIC in the database...");

          Optional<Mechanic> checkmech = mechanicRepository.findByPhonenumber(phonenumber);
          if(checkmech.isPresent()){
              System.out.println("SUCCESS: Mechanic found! Creating MyPrincipalMechanic object.");

              return new MyPrincipalMechanic(checkmech.get());
          }
          System.out.println("FAILURE: Mechanic not found with Phone Number: " + phonenumber);

          throw new UsernameNotFoundException("Mechanic not found with Phone Number: " + phonenumber);


      }
        System.out.println("ERROR: Invalid role specified: " + Role);

          throw new UsernameNotFoundException("Invalid role specified: " + Role);

    }
}


