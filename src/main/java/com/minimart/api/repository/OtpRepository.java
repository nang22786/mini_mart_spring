package com.minimart.api.repository;

import com.minimart.api.model.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {
    
    Optional<Otp> findByUserIdAndCodeAndVerified(Long userId, String code, Boolean verified);
    
    Optional<Otp> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}