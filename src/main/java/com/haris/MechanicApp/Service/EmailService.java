package com.haris.MechanicApp.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    JavaMailSender javaMailSender;



    public void sendVerificationEmail (String UserEmail , String UserToken){
        String verifyLink = "http://localhost:8080/api/verify/user/token?token=" + UserToken;
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(UserEmail);
        mail.setSubject("Verify Your Account");
        mail.setText("Click the link to verify your account:\n" + verifyLink +
                "\n\nYour Token: " + UserToken);
        javaMailSender.send(mail);



    }
}
