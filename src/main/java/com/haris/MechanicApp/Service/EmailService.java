package com.haris.MechanicApp.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    public void sendVerificationEmail(String userEmail, String otpCode) {

        String htmlContent = """
    <!DOCTYPE html>
    <html>
    <head>
        <style>
            body {
                font-family: Arial, sans-serif;
                background-color: #f4f6f8;
                padding: 20px;
            }
            .container {
                max-width: 500px;
                margin: auto;
                background: white;
                padding: 30px;
                border-radius: 8px;
                text-align: center;
            }
            h2 {
                color: #2c3e50;
            }
            p {
                color: #555;
            }
            .otp {
                font-size: 36px;
                font-weight: bold;
                letter-spacing: 8px;
                color: #fa0526;
                margin: 25px 0;
            }
            .footer {
                margin-top: 30px;
                font-size: 12px;
                color: #888;
            }
        </style>
    </head>
    <body>
        <div class="container">
            <h2>Verify Your Account</h2>
            <p>Thank you for registering with <b>OnFix</b>.</p>
            <p>Please use the following OTP to verify your account:</p>

            <div class="otp">%s</div>

            <p>This OTP is valid for a limited time.</p>

            <div class="footer">
                © 2026 OnFix. All rights reserved.
            </div>
        </div>
    </body>
    </html>
    """.formatted(otpCode);

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(userEmail);
            helper.setSubject("Your OTP Code - OnFix");
            helper.setText(htmlContent, true);

            javaMailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendForgotPasswordOTP (String userEmail, String otpCode){
        String htmlContent = """
    <!DOCTYPE html>
    <html>
    <head>
        <style>
            body {
                font-family: Arial, sans-serif;
                background-color: #f4f6f8;
                padding: 20px;
            }
            .container {
                max-width: 500px;
                margin: auto;
                background: white;
                padding: 30px;
                border-radius: 8px;
                text-align: center;
            }
            h2 {
                color: #2c3e50;
            }
            p {
                color: #555;
            }
            .otp {
                font-size: 36px;
                font-weight: bold;
                letter-spacing: 8px;
                color: #fa0526;
                margin: 25px 0;
            }
            .footer {
                margin-top: 30px;
                font-size: 12px;
                color: #888;
            }
        </style>
    </head>
    <body>
        <div class="container">
            <h4>Forgot Your Password, %s</h5>
            <p>Do not share OTP with anyone.</p>
            <p>Please use the following OTP to forgot your password:</p>

            <div class="otp">%s</div>

            <p>This OTP is valid for a limited time.</p>

            <div class="footer">
                © 2026 OnFix. All rights reserved.
            </div>
        </div>
    </body>
    </html>
    """.formatted(userEmail,otpCode);

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(userEmail);
            helper.setSubject("Forgot Password - OnFix");
            helper.setText(htmlContent, true);

            javaMailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }

    }

}
