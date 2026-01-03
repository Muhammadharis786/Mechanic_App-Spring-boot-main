package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Model.Verification.User;
import com.haris.MechanicApp.Service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

    @Autowired
    AdminService adminservice;

    @GetMapping("api/admin/alluser")
    public ResponseEntity<?> getAllUser(){
        return adminservice.alluser ();

    }

    @GetMapping("api/admin/allmechanics")
    public ResponseEntity<?> getAllMechanics(){
        return adminservice.allMechanics ();

    }




}
