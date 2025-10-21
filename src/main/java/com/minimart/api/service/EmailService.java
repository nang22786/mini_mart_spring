package com.minimart.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public boolean sendOtpEmail(String toEmail, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Mini Mart - Your OTP Verification Code");
            message.setText(buildOtpEmailBody(otpCode));

            mailSender.send(message);
            System.out.println("OTP email sent successfully to: " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendPasswordResetOtpEmail(String toEmail, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Mini Mart - Password Reset OTP");
            message.setText(buildPasswordResetEmailBody(otpCode));

            mailSender.send(message);
            System.out.println("Password reset OTP email sent successfully to: " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send password reset email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String buildOtpEmailBody(String otpCode) {
        return "Dear User,\n\n" +
                "Thank you for registering with Mini Mart!\n\n" +
                "Your OTP verification code is: " + otpCode + "\n\n" +
                "This code will expire in 5 minutes.\n\n" +
                "If you did not request this code, please ignore this email.\n\n" +
                "Best regards,\n" +
                "Mini Mart Team";
    }

    private String buildPasswordResetEmailBody(String otpCode) {
        return "Dear User,\n\n" +
                "You have requested to reset your password for your Mini Mart account.\n\n" +
                "Your password reset OTP code is: " + otpCode + "\n\n" +
                "This code will expire in 5 minutes.\n\n" +
                "If you did not request a password reset, please ignore this email and your password will remain unchanged.\n\n" +
                "Best regards,\n" +
                "Mini Mart Team";
    }
}