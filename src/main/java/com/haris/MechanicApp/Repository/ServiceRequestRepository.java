package com.haris.MechanicApp.Repository;

import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.RequestService.RequestService;
import com.haris.MechanicApp.Model.Verification.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ServiceRequestRepository extends JpaRepository<RequestService, Long> {

    @Modifying
    @Query("""
        UPDATE RequestService r
        SET r.mechanic = :mechanic,
            r.requestStatus = 'ACCEPTED'
        WHERE r.requestId = :requestId
          AND r.requestStatus = 'PENDING'
          AND r.mechanic IS NULL
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



    Optional<RequestService> findByRequestIdAndMechanic(Long requestId, Mechanic mechanic);

    Optional<RequestService> findByRequestIdAndUser(Long requestId, User user);
}
