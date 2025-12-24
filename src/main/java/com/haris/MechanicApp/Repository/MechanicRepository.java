package com.haris.MechanicApp.Repository;

import com.haris.MechanicApp.Controller.MechanicController;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MechanicRepository  extends JpaRepository<Mechanic ,Long> {
}
