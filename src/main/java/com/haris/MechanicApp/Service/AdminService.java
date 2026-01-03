package com.haris.MechanicApp.Service;

import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Verification.User;
import com.haris.MechanicApp.Repository.MechanicRepository;
import com.haris.MechanicApp.Repository.UserRepository;
import org.hibernate.type.ListType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

@Autowired
    UserRepository userRepo;

@Autowired
    MechanicRepository mechRepo;
    public ResponseEntity alluser() {

      List<User> allusers = userRepo.findAll ();

      return ResponseEntity.ok(allusers);

    }

    public ResponseEntity<?> allMechanics() {

        List<Mechanic> allmechanics =  mechRepo.findAll ();
        return ResponseEntity.ok(allmechanics);
    }
}
