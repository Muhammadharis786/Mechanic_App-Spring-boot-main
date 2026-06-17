package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/admin")
@CrossOrigin(origins = "*")  // dashboard ke liye
public class AdminController {

    @Autowired
    AdminService adminservice;

    // ── Dashboard Stats ──
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        return adminservice.getDashboardStats();
    }

    // ── Earnings Graph (last 6 months) ──
    @GetMapping("/earnings-graph")
    public ResponseEntity<?> getEarningsGraph() {
        return adminservice.getEarningsGraph();
    }

    // ── Users ──
    @GetMapping("/alluser")
    public ResponseEntity<?> getAllUser() {
        return adminservice.alluser();
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        return adminservice.deleteUser(userId);
    }

    @PutMapping("/user/{userId}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long userId) {
        return adminservice.toggleUserStatus(userId);
    }

    // ── Mechanics ──
    @GetMapping("/allmechanics")
    public ResponseEntity<?> getAllMechanics() {
        return adminservice.allMechanics();
    }

    @PutMapping("/mechanic/{mechanicId}/verify")
    public ResponseEntity<?> verifyMechanic(@PathVariable Long mechanicId) {
        return adminservice.verifyMechanic(mechanicId);
    }

    @PutMapping("/mechanic/{mechanicId}/unverify")
    public ResponseEntity<?> unverifyMechanic(@PathVariable Long mechanicId) {
        return adminservice.unverifyMechanic(mechanicId);
    }

    @DeleteMapping("/mechanic/{mechanicId}")
    public ResponseEntity<?> deleteMechanic(@PathVariable Long mechanicId) {
        return adminservice.deleteMechanic(mechanicId);
    }

    // ── Appointments ──
    @GetMapping("/appointments")
    public ResponseEntity<?> getAllAppointments() {
        return adminservice.allAppointments();
    }

    // ── Service Requests ──
    @GetMapping("/service-requests")
    public ResponseEntity<?> getAllServiceRequests() {
        return adminservice.allServiceRequests();
    }
}