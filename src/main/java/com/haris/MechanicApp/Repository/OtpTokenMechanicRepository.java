package com.haris.MechanicApp.Repository;

import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Verification.VerificationTokenMechanic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpTokenMechanicRepository  extends JpaRepository<VerificationTokenMechanic,Long> {
        Optional<VerificationTokenMechanic> findByMechanic(Mechanic mechanic);


        Optional<VerificationTokenMechanic> findByTokenAndMechanic_Phonenumber(String token, String phonenumber);
}
