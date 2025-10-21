package com.minimart.api.service;

import com.minimart.api.dto.AuthResponse;
import com.minimart.api.dto.ForgotPasswordRequest;
import com.minimart.api.dto.LoginRequest;
import com.minimart.api.dto.RegisterRequest;
import com.minimart.api.dto.ResendOtpRequest;
import com.minimart.api.dto.ResetPasswordRequest;
import com.minimart.api.dto.UpdateUserRequest;
import com.minimart.api.dto.UserDTO;
import com.minimart.api.dto.VerifyOtpRequest;
import com.minimart.api.model.Otp;
import com.minimart.api.model.User;
import com.minimart.api.repository.OtpRepository;
import com.minimart.api.repository.UserRepository;
import com.minimart.api.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private OtpRepository otpRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;
    
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return new AuthResponse(false, "Email already exists");
        }
        
        // Extract username from email (part before @)
        String username = request.getEmail().split("@")[0];
        
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUserName(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("customer");
        user.setStatus("inactive");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        
        String otpCode = generateOTP();
        Otp otp = new Otp();
        otp.setUserId(savedUser.getUserId());
        otp.setCode(otpCode);
        otp.setExpireAt(LocalDateTime.now().plusMinutes(1));
        otp.setVerified(false);
        otpRepository.save(otp);
        
        // Send OTP email
        boolean emailSent = emailService.sendOtpEmail(request.getEmail(), otpCode);
        
        if (emailSent) {
            System.out.println("OTP Code for " + request.getEmail() + ": " + otpCode);
            return new AuthResponse(true, "Registration successful. Please check your email for OTP verification code.");
        } else {
            System.out.println("Email failed. OTP Code for " + request.getEmail() + ": " + otpCode);
            return new AuthResponse(true, "Registration successful. OTP: " + otpCode + " (Email service unavailable, showing OTP here)");
        }
    }
    
    public AuthResponse resendOtp(ResendOtpRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        
        if (userOptional.isEmpty()) {
            return new AuthResponse(false, "User not found");
        }
        
        User user = userOptional.get();
        
        // Check if user is already verified
        if ("active".equals(user.getStatus())) {
            return new AuthResponse(false, "Account is already verified");
        }
        
        // Check for existing unverified OTP to prevent spam
        Optional<Otp> existingOtp = otpRepository.findTopByUserIdOrderByCreatedAtDesc(user.getUserId());
        if (existingOtp.isPresent() && !existingOtp.get().getVerified()) {
            LocalDateTime lastOtpTime = existingOtp.get().getCreatedAt();
            if (lastOtpTime.plusMinutes(1).isAfter(LocalDateTime.now())) {
                return new AuthResponse(false, "Please wait 1 minute before requesting a new OTP");
            }
        }
        
        // Generate and save new OTP
        String otpCode = generateOTP();
        Otp otp = new Otp();
        otp.setUserId(user.getUserId());
        otp.setCode(otpCode);
        otp.setExpireAt(LocalDateTime.now().plusMinutes(1));
        otp.setVerified(false);
        otpRepository.save(otp);
        
        // Send OTP email
        boolean emailSent = emailService.sendOtpEmail(request.getEmail(), otpCode);
        
        if (emailSent) {
            System.out.println("Resent OTP Code for " + request.getEmail() + ": " + otpCode);
            return new AuthResponse(true, "OTP has been resent to your email");
        } else {
            System.out.println("Email failed. Resent OTP Code for " + request.getEmail() + ": " + otpCode);
            return new AuthResponse(true, "OTP: " + otpCode + " (Email service unavailable, showing OTP here)");
        }
    }
    
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        
        if (userOptional.isEmpty()) {
            return new AuthResponse(false, "User not found");
        }
        
        User user = userOptional.get();
        
        Optional<Otp> otpOptional = otpRepository.findByUserIdAndCodeAndVerified(
            user.getUserId(), 
            request.getCode(), 
            false
        );
        
        if (otpOptional.isEmpty()) {
            return new AuthResponse(false, "Invalid or already used OTP");
        }
        
        Otp otp = otpOptional.get();
        
        if (otp.getExpireAt().isBefore(LocalDateTime.now())) {
            return new AuthResponse(false, "OTP has expired");
        }
        
        otp.setVerified(true);
        otpRepository.save(otp);
        
        user.setStatus("active");
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Generate access token
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getUserId(), user.getRole());
        
        UserDTO userDTO = convertToDTO(user);
        
        return new AuthResponse(true, "Account verified successfully", userDTO, accessToken);
    }
    
    public AuthResponse login(LoginRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        
        if (userOptional.isEmpty()) {
            return new AuthResponse(false, "Invalid email or password");
        }
        
        User user = userOptional.get();
        
        // Verify password using PasswordEncoder
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return new AuthResponse(false, "Invalid email or password");
        }
        
        if (!"active".equals(user.getStatus())) {
            return new AuthResponse(false, "Account is not active. Please verify your OTP first.");
        }
        
        // Generate access token
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getUserId(), user.getRole());
        
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        UserDTO userDTO = convertToDTO(user);
        
        return new AuthResponse(true, "Login successful", userDTO, accessToken);
    }
    
    public AuthResponse forgotPassword(ForgotPasswordRequest request) {
        // Check if email exists in database
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        
        if (userOptional.isEmpty()) {
            return new AuthResponse(false, "Email not found in our system");
        }
        
        User user = userOptional.get();
        
        // Check if user account is active
        if (!"active".equals(user.getStatus())) {
            return new AuthResponse(false, "Account is not active. Please verify your account first.");
        }
        
        Optional<Otp> existingOtp = otpRepository.findTopByUserIdOrderByCreatedAtDesc(user.getUserId());
        if (existingOtp.isPresent() && !existingOtp.get().getVerified()) {
            LocalDateTime lastOtpTime = existingOtp.get().getCreatedAt();
            if (lastOtpTime.plusMinutes(1).isAfter(LocalDateTime.now())) {
                return new AuthResponse(false, "Please wait 1 minute before requesting a new OTP");
            }
        }
        
        // Generate and save new OTP
        String otpCode = generateOTP();
        Otp otp = new Otp();
        otp.setUserId(user.getUserId());
        otp.setCode(otpCode);
        otp.setExpireAt(LocalDateTime.now().plusMinutes(1));
        otp.setVerified(false);
        otpRepository.save(otp);
        
        // Send OTP email
        boolean emailSent = emailService.sendPasswordResetOtpEmail(request.getEmail(), otpCode);
        
        if (emailSent) {
            System.out.println("Password Reset OTP for " + request.getEmail() + ": " + otpCode);
            return new AuthResponse(true, "Password reset OTP has been sent to your email");
        } else {
            System.out.println("Email failed. Password Reset OTP for " + request.getEmail() + ": " + otpCode);
            return new AuthResponse(true, "OTP: " + otpCode + " (Email service unavailable, showing OTP here)");
        }
    }
    
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        // Check if email exists in database
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        
        if (userOptional.isEmpty()) {
            return new AuthResponse(false, "Email not found in our system");
        }
        
        User user = userOptional.get();
        
        // Verify OTP
        Optional<Otp> otpOptional = otpRepository.findByUserIdAndCodeAndVerified(
            user.getUserId(), 
            request.getCode(), 
            false
        );
        
        if (otpOptional.isEmpty()) {
            return new AuthResponse(false, "Invalid or already used OTP");
        }
        
        Otp otp = otpOptional.get();
        
        if (otp.getExpireAt().isBefore(LocalDateTime.now())) {
            return new AuthResponse(false, "OTP has expired. Please request a new one.");
        }
        
        // Mark OTP as verified
        otp.setVerified(true);
        otpRepository.save(otp);
        
        // Update password with encryption
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        return new AuthResponse(true, "Password has been reset successfully. You can now login with your new password.");
    }
    
    public Optional<UserDTO> getUserInfo(Long userId) {
        return userRepository.findById(userId).map(this::convertToDTO);
    }
    
    public Optional<UserDTO> updateUserInfo(Long userId, String userName, String phone, String profileImage) {
        System.out.println("📝 Updating user info for ID: " + userId);
        
        return userRepository.findById(userId).map(user -> {
            if (userName != null && !userName.trim().isEmpty()) {
                System.out.println("   Updating name: " + userName);
                user.setUserName(userName);
            }
            
            // Update phone if provided
            if (phone != null && !phone.trim().isEmpty()) {
                System.out.println("   Updating phone: " + phone);
                user.setPhone(phone);
            }
            
            // Update profile image if provided
            if (profileImage != null && !profileImage.trim().isEmpty()) {
                System.out.println("   Updating profile image: " + profileImage);
                user.setProfileImage(profileImage);
            }
            
            user.setUpdatedAt(LocalDateTime.now());
            User updatedUser = userRepository.save(user);
            
            System.out.println("✅ User info updated successfully");
            
            return convertToDTO(updatedUser);
        });
    }
    
    private String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
    
    public Optional<UserDTO> getUserById(Long userId) {
        return userRepository.findById(userId).map(this::convertToDTO);
    }
    
    public Optional<UserDTO> getUserByEmail(String email) {
        return userRepository.findByEmail(email).map(this::convertToDTO);
    }
    
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public boolean deleteUser(Long userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
            return true;
        }
        return false;
    }
    
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setUserName(user.getUserName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setProfileImage(user.getProfileImage());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}