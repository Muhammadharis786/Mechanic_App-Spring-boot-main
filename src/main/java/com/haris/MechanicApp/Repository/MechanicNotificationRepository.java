package com.haris.MechanicApp.Repository;

import com.haris.MechanicApp.Model.Notification.MechanicNotification;
import org.springframework.data.jpa.repository.*;

import javax.management.Notification;
import java.util.List;


public interface MechanicNotificationRepository extends JpaRepository<MechanicNotification, Long> {
    List<MechanicNotification> findByMechanicIdOrderByCreatedAtDesc(Long mechanicId);
}