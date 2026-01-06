package com.haris.MechanicApp.Repository;

import com.haris.MechanicApp.Controller.MechanicController;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Verification.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MechanicRepository  extends JpaRepository<Mechanic ,Long> {

    Optional<Mechanic>  findByPhonenumber(String number);

    boolean existsByUser(User user);
}
