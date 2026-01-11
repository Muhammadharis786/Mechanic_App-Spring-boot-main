package com.haris.MechanicApp.Repository;

import com.haris.MechanicApp.Model.Verification.User;
import com.haris.MechanicApp.Model.Verification.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface VerificationTokenRepository  extends JpaRepository<VerificationToken , Long> {


    Optional<VerificationToken> findByUser(User user);





    Optional<VerificationToken> findByTokenAndUser_Userid(String token, long userid);

    List<VerificationToken> findAllByCreatedDateBefore(LocalDateTime threshold);
}
