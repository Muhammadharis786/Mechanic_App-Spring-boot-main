package com.haris.MechanicApp.Repository;

import com.haris.MechanicApp.Model.Verification.User;
import com.haris.MechanicApp.Model.Verification.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository  extends JpaRepository<VerificationToken , Long> {


    Optional<VerificationToken> findByUser(User user);

//    @Query("SELECT v FROM VerificationToken v JOIN v.user u WHERE v.token = :token")
//    Optional<VerificationToken> findByToken(@Param("token") String token);



    Optional<VerificationToken> findByTokenAndUser_Id(String token, long userid);
}
