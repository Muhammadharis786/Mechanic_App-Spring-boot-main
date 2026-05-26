package com.haris.MechanicApp.Repository;

import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.RequestService.RequestService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

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
}