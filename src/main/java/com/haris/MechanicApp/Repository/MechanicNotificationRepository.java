package com.haris.MechanicApp.Repository;

import com.haris.MechanicApp.Model.Appointments.Appointments;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Notification.Notification;
import com.haris.MechanicApp.Model.Notification.NotificationType;
import org.springframework.data.jpa.repository.*;

import java.util.List;
import java.util.Optional;


public interface MechanicNotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByMechanicIdOrderByCreatedAtDesc(Long mechanicId);

    Optional<Notification> findByIdAndMechanic(long notificationid , Mechanic mechanic);

    List<Notification> findByMechanic(Mechanic mechanic);

    List<Notification> findByAppointments(Appointments appointments);

    Optional<Notification> findByMechanicAndAppointments(Mechanic mechanic, Appointments appointments1);

    long countByAppointments(Appointments appointments);

    List<Notification> findByUser_UseridOrderByCreatedAtDesc(Long userid);

    List<Notification> findByMechanic_IdAndTypeInOrderByCreatedAtDesc(Long id, List<NotificationType> notificationTypes);

    List<Notification> findByUser_UseridAndTypeInOrderByCreatedAtDesc(long userid, List<NotificationType> notificationTypes);
}