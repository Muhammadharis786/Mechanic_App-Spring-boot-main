package com.haris.MechanicApp.Repository;

import com.haris.MechanicApp.Model.RequestService.RequestService;
import com.haris.MechanicApp.Model.Review.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {



    boolean existsByServiceId(Long requestId);

    boolean existsByAppointmentId(String appointmentId);
}