package com.haris.MechanicApp.Service;

import com.haris.MechanicApp.Model.Appointments.AppointmentStatus;
import com.haris.MechanicApp.Model.Appointments.Appointments;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.RequestService.RequestService;
import com.haris.MechanicApp.Model.RequestService.ServiceRequestStatus;
import com.haris.MechanicApp.Model.Verification.User;
import com.haris.MechanicApp.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    UserRepository userRepo;

    @Autowired
    MechanicRepository mechRepo;

    @Autowired
    AppointmentRepository appointmentRepo;

    @Autowired
    ServiceRequestRepository serviceRequestRepo;

    // ─────────────────────────────────────────────
    // DASHBOARD STATS
    // ─────────────────────────────────────────────

    public ResponseEntity<?> getDashboardStats() {
        long totalUsers     = userRepo.count();
        long totalMechanics = mechRepo.count();
        long totalApps      = appointmentRepo.count();
        long totalRequests  = serviceRequestRepo.count();

        // Total earnings (appointments + service requests)
        List<Appointments> completedApps = appointmentRepo.findByStatus(AppointmentStatus.COMPLETED);
        List<RequestService> completedReqs = serviceRequestRepo.findByRequestStatus(ServiceRequestStatus.COMPLETED);

        double totalEarnings = 0.0;
        for (Appointments a : completedApps) {
            if (a.getAmount() != null) totalEarnings += a.getAmount().doubleValue();
        }
        for (RequestService r : completedReqs) {
            if (r.getFinalAmount() != null) totalEarnings += r.getFinalAmount();
        }

        // Today earnings
        Instant startOfDay = java.time.LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay   = java.time.LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        double todayEarnings = 0.0;
        for (Appointments a : completedApps) {
            if (a.getCompletedAt() != null && a.getAmount() != null
                    && a.getCompletedAt().isAfter(startOfDay) && a.getCompletedAt().isBefore(endOfDay)) {
                todayEarnings += a.getAmount().doubleValue();
            }
        }
        for (RequestService r : completedReqs) {
            if (r.getCompletedAt() != null && r.getFinalAmount() != null
                    && r.getCompletedAt().isAfter(startOfDay) && r.getCompletedAt().isBefore(endOfDay)) {
                todayEarnings += r.getFinalAmount();
            }
        }

        // This month earnings
        YearMonth thisMonth = YearMonth.now();
        Instant monthStart  = thisMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant monthEnd    = thisMonth.atEndOfMonth().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        double thisMonthEarnings = 0.0;
        for (Appointments a : completedApps) {
            if (a.getCompletedAt() != null && a.getAmount() != null
                    && a.getCompletedAt().isAfter(monthStart) && a.getCompletedAt().isBefore(monthEnd)) {
                thisMonthEarnings += a.getAmount().doubleValue();
            }
        }
        for (RequestService r : completedReqs) {
            if (r.getCompletedAt() != null && r.getFinalAmount() != null
                    && r.getCompletedAt().isAfter(monthStart) && r.getCompletedAt().isBefore(monthEnd)) {
                thisMonthEarnings += r.getFinalAmount();
            }
        }

        // Last month earnings
        YearMonth lastMonth     = thisMonth.minusMonths(1);
        Instant lastMonthStart  = lastMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant lastMonthEnd    = lastMonth.atEndOfMonth().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        double lastMonthEarnings = 0.0;
        for (Appointments a : completedApps) {
            if (a.getCompletedAt() != null && a.getAmount() != null
                    && a.getCompletedAt().isAfter(lastMonthStart) && a.getCompletedAt().isBefore(lastMonthEnd)) {
                lastMonthEarnings += a.getAmount().doubleValue();
            }
        }
        for (RequestService r : completedReqs) {
            if (r.getCompletedAt() != null && r.getFinalAmount() != null
                    && r.getCompletedAt().isAfter(lastMonthStart) && r.getCompletedAt().isBefore(lastMonthEnd)) {
                lastMonthEarnings += r.getFinalAmount();
            }
        }

        // Verified mechanics count
        long verifiedMechanics   = mechRepo.findAll().stream().filter(Mechanic::isIsverified).count();
        long unverifiedMechanics = totalMechanics - verifiedMechanics;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalMechanics", totalMechanics);
        stats.put("verifiedMechanics", verifiedMechanics);
        stats.put("unverifiedMechanics", unverifiedMechanics);
        stats.put("totalAppointments", totalApps);
        stats.put("totalServiceRequests", totalRequests);
        stats.put("totalEarnings", totalEarnings);
        stats.put("todayEarnings", todayEarnings);
        stats.put("thisMonthEarnings", thisMonthEarnings);
        stats.put("lastMonthEarnings", lastMonthEarnings);

        return ResponseEntity.ok(stats);
    }

    // ─────────────────────────────────────────────
    // MONTHLY EARNINGS GRAPH (last 6 months)
    // ─────────────────────────────────────────────

    public ResponseEntity<?> getEarningsGraph() {
        List<Map<String, Object>> graph = new ArrayList<>();
        YearMonth current = YearMonth.now();

        List<Appointments>    allApps = appointmentRepo.findByStatus(AppointmentStatus.COMPLETED);
        List<RequestService>  allReqs = serviceRequestRepo.findByRequestStatus(ServiceRequestStatus.COMPLETED);

        for (int i = 5; i >= 0; i--) {
            YearMonth ym    = current.minusMonths(i);
            Instant start   = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant end     = ym.atEndOfMonth().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

            double earning = 0.0;
            for (Appointments a : allApps) {
                if (a.getCompletedAt() != null && a.getAmount() != null
                        && a.getCompletedAt().isAfter(start) && a.getCompletedAt().isBefore(end)) {
                    earning += a.getAmount().doubleValue();
                }
            }
            for (RequestService r : allReqs) {
                if (r.getCompletedAt() != null && r.getFinalAmount() != null
                        && r.getCompletedAt().isAfter(start) && r.getCompletedAt().isBefore(end)) {
                    earning += r.getFinalAmount();
                }
            }

            Map<String, Object> point = new HashMap<>();
            point.put("month", ym.getMonth().name().substring(0, 3) + " " + ym.getYear());
            point.put("earnings", earning);
            graph.add(point);
        }

        return ResponseEntity.ok(graph);
    }

    // ─────────────────────────────────────────────
    // USERS
    // ─────────────────────────────────────────────

    public ResponseEntity<?> alluser() {
        List<User> users = userRepo.findAll();
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("userid", u.getUserid());
            map.put("username", u.getUsername());
            map.put("email", u.getEmail());
            map.put("phonenumber", u.getPhonenumber());
            map.put("enabled", u.isEnabled());
            map.put("registrationDate", u.getRegistrationDate());
            map.put("roles", u.getRoles());
            map.put("imageUrl", u.getUserimgurl());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    public ResponseEntity<?> deleteUser(Long userId) {
        Optional<User> user = userRepo.findById(userId);
        if (user.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        userRepo.delete(user.get());
        return ResponseEntity.ok("User deleted successfully");
    }

    public ResponseEntity<?> toggleUserStatus(Long userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        User user = userOpt.get();
        user.setEnabled(!user.isEnabled());
        userRepo.save(user);
        return ResponseEntity.ok(Map.of("enabled", user.isEnabled(), "message",
                user.isEnabled() ? "User enabled" : "User disabled"));
    }

    // ─────────────────────────────────────────────
    // MECHANICS
    // ─────────────────────────────────────────────

    public ResponseEntity<?> allMechanics() {
        List<Mechanic> mechanics = mechRepo.findAll();
        List<Map<String, Object>> result = mechanics.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("name", m.getName());
            map.put("phonenumber", m.getPhonenumber());
            map.put("mechanictype", m.getMechanictype());
            map.put("shopaddress", m.getShopaddress());
            map.put("experienceyears", m.getExperienceyears());
            map.put("isverified", m.isIsverified());
            map.put("isactive", m.isIsactive());
            map.put("isengaged", m.isIsengaged());
            map.put("averageRating", m.getAverageRating());
            map.put("totalJobsCompleted", m.getTotalJobsCompleted());
            map.put("totalJobsCancelled", m.getTotalJobsCancelled());
            map.put("totalearning", m.getTotalearning());
            map.put("totalReviews", m.getTotalReviews());
            map.put("mechanicimgurl", m.getMechanicimgurl());
            map.put("iscompleteRegister", m.isIscompleteRegister());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    public ResponseEntity<?> verifyMechanic(Long mechanicId) {
        Optional<Mechanic> mechOpt = mechRepo.findById(mechanicId);
        if (mechOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic not found");
        Mechanic mechanic = mechOpt.get();
        mechanic.setIsverified(true);
        mechRepo.save(mechanic);
        return ResponseEntity.ok("Mechanic verified successfully");
    }

    public ResponseEntity<?> unverifyMechanic(Long mechanicId) {
        Optional<Mechanic> mechOpt = mechRepo.findById(mechanicId);
        if (mechOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic not found");
        Mechanic mechanic = mechOpt.get();
        mechanic.setIsverified(false);
        mechRepo.save(mechanic);
        return ResponseEntity.ok("Mechanic unverified");
    }

    public ResponseEntity<?> deleteMechanic(Long mechanicId) {
        Optional<Mechanic> mechOpt = mechRepo.findById(mechanicId);
        if (mechOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic not found");
        mechRepo.delete(mechOpt.get());
        return ResponseEntity.ok("Mechanic deleted successfully");
    }

    // ─────────────────────────────────────────────
    // APPOINTMENTS
    // ─────────────────────────────────────────────

    public ResponseEntity<?> allAppointments() {
        List<Appointments> appointments = appointmentRepo.findAll();
        List<Map<String, Object>> result = appointments.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("appointmentId", a.getAppointmentId());
            map.put("serviceType", a.getServiceType());
            map.put("status", a.getStatus());
            map.put("appointmentDate", a.getAppointmentDate());
            map.put("appointmentTime", a.getAppointmentTime());
            map.put("amount", a.getAmount());
            map.put("address", a.getAddress());
            map.put("completedAt", a.getCompletedAt());
            map.put("createdAt", a.getCreatedAt());
            map.put("username", a.getUser() != null ? a.getUser().getUsername() : "N/A");
            map.put("userPhone", a.getUser() != null ? a.getUser().getPhonenumber() : "N/A");
            map.put("mechanicName", a.getMechanic() != null ? a.getMechanic().getName() : "Not Assigned");
            map.put("mechanicPhone", a.getMechanic() != null ? a.getMechanic().getPhonenumber() : "N/A");
            map.put("paymentStatus", a.getPaymentStatus());
            map.put("paymentMethod", a.getPaymentMethod());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────
    // SERVICE REQUESTS
    // ─────────────────────────────────────────────

    public ResponseEntity<?> allServiceRequests() {
        List<RequestService> requests = serviceRequestRepo.findAll();
        List<Map<String, Object>> result = requests.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("requestId", r.getRequestId());
            map.put("serviceType", r.getServiceType());
            map.put("requestStatus", r.getRequestStatus());
            map.put("finalAmount", r.getFinalAmount());
            map.put("locationName", r.getLocationName());
            map.put("createdAt", r.getCreatedAt());
            map.put("completedAt", r.getCompletedAt());
            map.put("paymentStatus", r.getPaymentStatus());
            map.put("username", r.getUser() != null ? r.getUser().getUsername() : "N/A");
            map.put("userPhone", r.getUser() != null ? r.getUser().getPhonenumber() : "N/A");
            map.put("mechanicName", r.getMechanic() != null ? r.getMechanic().getName() : "Not Assigned");
            map.put("mechanicPhone", r.getMechanic() != null ? r.getMechanic().getPhonenumber() : "N/A");
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}