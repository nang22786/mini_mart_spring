package com.minimart.api.service;

import com.minimart.api.dto.AdvertisingDTO;
import com.minimart.api.model.Advertising;
import com.minimart.api.repository.AdvertisingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdvertisingService {

    @Autowired
    private AdvertisingRepository advertisingRepository;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Create new advertising with image upload
     */
    @Transactional
    public AdvertisingDTO createAdvertising(MultipartFile imageFile) {
        System.out.println("📢 Creating new advertising");

        String cloudinaryUrl = null;
        try {
            // Upload image to Cloudinary
            cloudinaryUrl = fileStorageService.storeFile(imageFile, "advertising");
            System.out.println("✅ Image uploaded to Cloudinary: " + cloudinaryUrl);

            // Create advertising entity
            Advertising advertising = new Advertising();
            advertising.setImageUrl(cloudinaryUrl); // Store full Cloudinary URL

            // Save to database
            Advertising saved = advertisingRepository.save(advertising);
            System.out.println("✅ Advertising created with ID: " + saved.getId());
            return convertToDTO(saved);

        } catch (Exception e) {
            // If an error occurs, delete uploaded image from Cloudinary
            if (cloudinaryUrl != null) {
                try {
                    fileStorageService.deleteFile(cloudinaryUrl);
                    System.out.println("🧹 Rolled back image from Cloudinary after error");
                } catch (Exception delEx) {
                    System.err.println("⚠️ Failed to delete uploaded file after error: " + delEx.getMessage());
                }
            }
            throw new RuntimeException("Failed to create advertising: " + e.getMessage(), e);
        }
    }

    /**
     * Toggle advertising active status
     */
    @Transactional
    public AdvertisingDTO toggleAdvertisingStatus(Integer id, Boolean isActive) {
        System.out.println("🔄 Toggling advertising status for ID: " + id + " to " + isActive);

        Advertising advertising = advertisingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertising not found with id: " + id));

        advertising.setIsActive(isActive);
        Advertising updated = advertisingRepository.save(advertising);

        System.out.println("✅ Advertising status updated");
        return convertToDTO(updated);
    }

    /**
     * Get all advertising (sorted by newest first)
     */
    public List<AdvertisingDTO> getAllAdvertising() {
        System.out.println("📋 Fetching all advertising");
        return advertisingRepository.findAllByOrderByCreateDateDesc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get advertising by ID
     */
    public AdvertisingDTO getAdvertisingById(Integer id) {
        System.out.println("🔍 Fetching advertising with ID: " + id);
        Advertising advertising = advertisingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertising not found with id: " + id));
        return convertToDTO(advertising);
    }

    /**
     * Update advertising image
     */
    @Transactional
    public AdvertisingDTO updateAdvertisingImage(Integer id, MultipartFile imageFile) {
        System.out.println("🔄 Updating advertising image for ID: " + id);

        Advertising advertising = advertisingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertising not found with id: " + id));

        String oldImageUrl = advertising.getImageUrl();
        String newImageUrl = null;

        try {
            // 🗑️ DELETE OLD IMAGE FROM CLOUDINARY
            if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                fileStorageService.deleteFile(oldImageUrl);
                System.out.println("✅ Old image deleted from Cloudinary: " + oldImageUrl);
            }

            // ⬆️ UPLOAD NEW IMAGE TO CLOUDINARY
            newImageUrl = fileStorageService.storeFile(imageFile, "advertising");
            System.out.println("✅ New image uploaded to Cloudinary: " + newImageUrl);

            // Update entity with new Cloudinary URL
            advertising.setImageUrl(newImageUrl);
            Advertising updated = advertisingRepository.save(advertising);

            System.out.println("✅ Advertising image updated successfully");
            return convertToDTO(updated);

        } catch (Exception e) {
            // Rollback: Delete new image if update fails
            if (newImageUrl != null) {
                try {
                    fileStorageService.deleteFile(newImageUrl);
                    System.out.println("🧹 Rolled back new image after update error");
                } catch (Exception delEx) {
                    System.err.println("⚠️ Failed to rollback uploaded image: " + delEx.getMessage());
                }
            }
            throw new RuntimeException("Failed to update advertising image: " + e.getMessage(), e);
        }
    }

    /**
     * Delete advertising and its image file from Cloudinary
     */
    @Transactional
    public void deleteAdvertising(Integer id) {
        System.out.println("🗑️ Deleting advertising with ID: " + id);

        Advertising advertising = advertisingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertising not found with id: " + id));

        // 🗑️ DELETE IMAGE FROM CLOUDINARY
        String imageUrl = advertising.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                fileStorageService.deleteFile(imageUrl);
                System.out.println("✅ Image deleted from Cloudinary: " + imageUrl);
            } catch (Exception e) {
                System.err.println("⚠️ Could not delete image from Cloudinary: " + e.getMessage());
                // Continue with database deletion even if Cloudinary delete fails
            }
        }

        // Delete from database
        advertisingRepository.deleteById(id);
        System.out.println("✅ Advertising deleted from database");
    }

    /**
     * Convert entity to DTO
     */
    private AdvertisingDTO convertToDTO(Advertising advertising) {
        AdvertisingDTO dto = new AdvertisingDTO();
        dto.setId(advertising.getId());
        dto.setImageUrl(advertising.getImageUrl()); // Full Cloudinary URL
        dto.setCreateDate(advertising.getCreateDate());
        dto.setIsActive(advertising.getIsActive());
        return dto;
    }
}