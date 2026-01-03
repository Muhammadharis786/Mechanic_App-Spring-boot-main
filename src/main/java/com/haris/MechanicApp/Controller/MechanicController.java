package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Repository.MechanicRepository;
import com.haris.MechanicApp.Service.MechanicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MechanicController {

    @Autowired
    private MechanicRepository mechanicRepo;

    @Autowired
    private MechanicService mechanicService;
    @GetMapping ("api/mechanic/allmechanic")
    public List<Mechanic> getAllMechanic(){
    return mechanicRepo.findAll();
 }


 //ye check kraiga kay mechanic online hay offline
    @PostMapping("api/mechanic/checkinginternet/{id}/heartbeat") public void heartbeat(@PathVariable Long id) {


        mechanicService.updateLastSeen(id); }

    @PostMapping ("api/mechanic/jobstatus")
    public ResponseEntity<?> updateEngaged(
            @PathVariable Long id,
            @RequestParam boolean engaged) {

      mechanicService.updateEngagedStatus(id, engaged);
        return ResponseEntity.ok("Mechanic status updated");
    }
}


