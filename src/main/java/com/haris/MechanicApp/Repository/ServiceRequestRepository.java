package com.haris.MechanicApp.Repository;

import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.RequestService.RequestService;
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

    @Query("""
        SELECT r FROM RequestService r
        WHERE r.mechanic.id = :mechanicId
        AND r.requestStatus IN ('ACCEPTED', 'MECHANIC_ON_WAY')
    """)
    Optional<RequestService> findActiveAcceptedRequestByMechanicId(
            @Param("mechanicId") Long mechanicId
    );
}
