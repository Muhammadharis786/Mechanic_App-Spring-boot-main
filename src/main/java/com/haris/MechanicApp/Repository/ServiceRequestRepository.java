package com.haris.MechanicApp.Repository;

import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.RequestService.RequestService;
import com.haris.MechanicApp.Model.RequestService.ServiceRequestStatus;
import com.haris.MechanicApp.Model.Verification.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ServiceRequestRepository extends JpaRepository<RequestService, Long> {

    @Modifying
    @Query("""
        UPDATE RequestService r
        SET r.mechanic = :mechanic,
            r.requestStatus = 'ACCEPTED'
        WHERE r.requestId = :requestId
          AND r.requestStatus = 'PENDING'
    """)
    int acceptRequest(Long requestId, Mechanic mechanic);

    @Query(value = """
        SELECT * FROM service_requests r
        WHERE r.mechanic_id = :mechanicId
        AND r.request_status IN ('ACCEPTED', 'MECHANIC_ON_WAY')
        ORDER BY r.request_id DESC
        LIMIT 1
    """, nativeQuery = true)
    Optional<RequestService> findActiveAcceptedRequestByMechanicId(
            @Param("mechanicId") Long mechanicId
    );

    // ServiceRequestRepository.java

    @Query("SELECT r FROM RequestService r WHERE r.mechanic = :mechanic " +
            "AND r.requestStatus = 'COMPLETED' " +
            "AND r.completedAt >= :startOfDay " +
            "AND r.completedAt < :endOfDay")
    List<RequestService> findTodayCompletedByMechanic(
            @Param("mechanic") Mechanic mechanic,
            @Param("startOfDay") Instant startOfDay,
            @Param("endOfDay") Instant endOfDay
    );



    Optional<RequestService> findByRequestIdAndMechanic(Long requestId, Mechanic mechanic);

    Optional<RequestService> findByRequestIdAndUser(Long requestId, User user);

    List<RequestService> findTop5ByMechanicAndRequestStatusOrderByCompletedAtDesc(
            Mechanic mechanic, ServiceRequestStatus status
    );
    // Woh saari PENDING requests jo :cutoff se pehle bani hain aur kisi mechanic
    // ne accept nahi ki (mechanic IS NULL)
    @Query("""
        SELECT r FROM RequestService r
        WHERE r.requestStatus = 'PENDING'
          AND r.mechanic IS NULL
          AND r.createdAt <= :cutoff
    """)
    List<RequestService> findExpiredPendingRequests(@Param("cutoff") Instant cutoff);


    // My Services page ke liye count

    List<RequestService> findByMechanicAndRequestStatusOrderByCompletedAtDesc(Mechanic mech, ServiceRequestStatus serviceRequestStatus);


    // 1. Active service requests count
    @Query("SELECT COUNT(r) FROM RequestService r WHERE r.mechanic = :mechanic " +
            "AND r.requestStatus IN ('ACCEPTED', 'MECHANIC_ON_WAY', 'ARRIVED', 'INSPECTION_STARTED', " +
            "'PRICE_GIVEN', 'IN_PROGRESS', 'WAITING_USER_APPROVAL', 'APPROVED_PRICE_REQUEST', " +
            "'WORK_STARTED', 'WORK_COMPLETED', 'PAYMENT_PENDING')")
    long countActiveByMechanic(@Param("mechanic") Mechanic mechanic);

    // 2. Monthly earnings filter ke liye
    @Query("SELECT r FROM RequestService r WHERE r.mechanic = :mechanic " +
            "AND r.requestStatus = 'COMPLETED' " +
            "AND r.completedAt >= :start AND r.completedAt < :end")
    List<RequestService> findCompletedByMechanicAndMonth(@Param("mechanic") Mechanic mechanic,
                                                         @Param("start") Instant start,
                                                         @Param("end") Instant end);

    // 3. This month/last month count (for growth %)
    @Query("SELECT COUNT(r) FROM RequestService r WHERE r.mechanic = :mechanic " +
            "AND r.createdAt >= :start AND r.createdAt < :end")
    long countByMechanicAndCreatedAtBetween(@Param("mechanic") Mechanic mechanic,
                                            @Param("start") Instant start,
                                            @Param("end") Instant end);

    List<RequestService> findByRequestStatus(ServiceRequestStatus status);
}
