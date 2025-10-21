package com.minimart.api.controller;

import com.minimart.api.dto.AuthResponse;
import com.minimart.api.dto.ForgotPasswordRequest;
import com.minimart.api.dto.LoginRequest;
import com.minimart.api.dto.RegisterRequest;
import com.minimart.api.dto.ResendOtpRequest;
import com.minimart.api.dto.ResetPasswordRequest;
import com.minimart.api.dto.UserDTO;
import com.minimart.api.dto.VerifyOtpRequest;
import com.minimart.api.service.UserService;
import com.minimart.api.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);

        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        AuthResponse response = userService.verifyOtp(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<AuthResponse> resendOtp(@RequestBody ResendOtpRequest request) {
        AuthResponse response = userService.resendOtp(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        AuthResponse response = userService.forgotPassword(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        AuthResponse response = userService.resetPassword(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/info/{userId}")
    public ResponseEntity<UserDTO> getUserInfo(@PathVariable Long userId) {
        return userService.getUserInfo(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update user profile with optional image upload
     * Accepts: userName, phone_number, and image (all optional)
     * Using POST because multipart/form-data doesn't work well with PUT in Spring Boot
     */
    @PostMapping("/info/{userId}")
    public ResponseEntity<Map<String, Object>> updateUserInfo(
            @PathVariable Long userId,
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "phone_number", required = false) String phoneNumber,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        
        Map<String, Object> response = new HashMap<>();
        String uploadedFilename = null;
        
        try {
            System.out.println("=== UPDATE USER PROFILE ===");
            System.out.println("User ID: " + userId);
            System.out.println("User Name: " + userName);
            System.out.println("Phone Number: " + phoneNumber);
            System.out.println("Has Image: " + (image != null && !image.isEmpty()));
            
            // Get existing user to check for old profile image
            Optional<UserDTO> existingUserOpt = userService.getUserById(userId);
            if (existingUserOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            UserDTO existingUser = existingUserOpt.get();
            String oldImageUrl = existingUser.getProfileImage();
            String imageUrl = oldImageUrl; // Keep old image by default
            
            // Upload new image if provided
            if (image != null && !image.isEmpty()) {
                System.out.println("📤 Uploading new image: " + image.getOriginalFilename());
                
                // Store new file with "profile" type
                uploadedFilename = fileStorageService.storeFile(image, "profile");
                
                // Generate RELATIVE URL (without localhost)
                imageUrl = "/api/files/profile/" + uploadedFilename;
                
                System.out.println("✅ New image uploaded: " + imageUrl);
            }
            
            // Update user profile
            Optional<UserDTO> updatedUserOpt = userService.updateUserInfo(
                userId, 
                userName, 
                phoneNumber, 
                imageUrl
            );
            
            if (updatedUserOpt.isPresent()) {
                // Delete old image ONLY AFTER successful update
                if (uploadedFilename != null && oldImageUrl != null && !oldImageUrl.isEmpty()) {
                    String oldFilename = extractFilenameFromUrl(oldImageUrl);
                    if (oldFilename != null && !oldFilename.isEmpty()) {
                        try {
                            fileStorageService.deleteFile(oldFilename, "profile");
                            System.out.println("🗑️ Deleted old profile image: " + oldFilename);
                        } catch (Exception e) {
                            System.err.println("⚠️ Failed to delete old image: " + e.getMessage());
                        }
                    }
                }
                
                response.put("success", true);
                response.put("message", "Profile updated successfully");
                response.put("user", updatedUserOpt.get());
                
                System.out.println("✅ Profile updated successfully");
                
                return ResponseEntity.ok(response);
            } else {
                // Rollback: delete newly uploaded image if update failed
                if (uploadedFilename != null) {
                    try {
                        fileStorageService.deleteFile(uploadedFilename, "profile");
                        System.out.println("🗑️ Rolled back uploaded image: " + uploadedFilename);
                    } catch (Exception ex) {
                        System.err.println("⚠️ Failed to delete uploaded image: " + ex.getMessage());
                    }
                }
                
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error updating profile: " + e.getMessage());
            e.printStackTrace();
            
            // Rollback: delete newly uploaded image if error occurred
            if (uploadedFilename != null) {
                try {
                    fileStorageService.deleteFile(uploadedFilename, "profile");
                    System.out.println("🗑️ Rolled back uploaded image: " + uploadedFilename);
                } catch (Exception ex) {
                    System.err.println("⚠️ Failed to delete uploaded image: " + ex.getMessage());
                }
            }
            
            response.put("success", false);
            response.put("message", "Failed to update profile: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserDTO> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userService.deleteUser(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Helper method to extract filename from URL
    private String extractFilenameFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        
        int lastSlashIndex = imageUrl.lastIndexOf('/');
        if (lastSlashIndex != -1 && lastSlashIndex < imageUrl.length() - 1) {
            return imageUrl.substring(lastSlashIndex + 1);
        }
        
        return null;
    }
}