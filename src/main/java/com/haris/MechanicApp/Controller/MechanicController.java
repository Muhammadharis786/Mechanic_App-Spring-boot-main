package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Repository.MechanicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MechanicController {

    @Autowired
    private MechanicRepository mechanicRepo;
    @GetMapping ("api/mechanic/allmechanic")
    public List<Mechanic> getAllMechanic(){
    return mechanicRepo.findAll();
 }
}
