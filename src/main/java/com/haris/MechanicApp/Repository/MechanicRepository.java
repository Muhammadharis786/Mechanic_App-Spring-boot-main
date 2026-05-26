package com.haris.MechanicApp.Repository;

import com.haris.MechanicApp.Controller.MechanicController;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Verification.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MechanicRepository  extends JpaRepository<Mechanic ,Long> {

    Optional<Mechanic>  findByPhonenumber(String number);

    boolean existsByIdAndIsverifiedTrueAndIsengagedFalseAndMechanictypeIgnoreCase(
            Long id,
            String mechanictype
    );

    @Query("""
    SELECT m.id
    FROM Mechanic m
    WHERE m.id IN :ids
      AND m.isverified = true
      AND m.isengaged = false
     
""")
    List<Long> findAvailableMechanicIds(
            List<Long> ids
    );
    boolean existsByUser(User user);
}
