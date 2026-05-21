package com.haris.MechanicApp.Repository;

import com.haris.MechanicApp.Model.FCM.FcmToken;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Verification.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByToken(String token);

    List<FcmToken> findByUser(User user);

    List<FcmToken> findByMechanic(Mechanic mechanic);

    void deleteByToken(String token);
}