package com.haris.MechanicApp.Service;

import com.haris.MechanicApp.Model.Verification.VerificationToken;
import com.haris.MechanicApp.Repository.UserRepository;
import com.haris.MechanicApp.Repository.VerificationTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class UserCleanupService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private VerificationTokenRepository tokenRepo;

    @Scheduled(fixedRate = 60000)
    @Transactional

    public void deleteUnverifiedUser (){

        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        List<VerificationToken> allunverifeduser= tokenRepo.findAllByCreatedDateBefore(oneMinuteAgo);

        for(VerificationToken tokenunverified : allunverifeduser)
        {

            tokenRepo.delete(tokenunverified);
            System.out.println("delete unverified token after one minute");
        }

    }


}
