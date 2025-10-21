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

        String fileName = null;
        try {
            // Upload image to folder
            fileName = fileStorageService.storeFile(imageFile, "advertising");
            String imageUrl = "/api/files/advertising/" + fileName;

            // Create advertising entity
            Advertising advertising = new Advertising();
            advertising.setImageUrl(imageUrl);

            // Save to database
            Advertising saved = advertisingRepository.save(advertising);
            System.out.println("✅ Advertising created with ID: " + saved.getId());
            return convertToDTO(saved);

        } catch (Exception e) {
            // If an error occurs, delete uploaded image
            if (fileName != null) {
                try {
                    fileStorageService.deleteFile(fileName, "advertising");
                    System.out.println("🧹 Rolled back image file after error: " + fileName);
                } catch (Exception delEx) {
                    System.err.println("⚠️ Failed to delete uploaded file after error: " + delEx.getMessage());
                }
            }
            throw new RuntimeException("Failed to create advertising: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    public AdvertisingDTO toggleAdvertisingStatus(Integer id, Boolean isActive) {
        Advertising advertising = advertisingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertising not found with id: " + id));
        advertising.setIsActive(isActive);
        Advertising updated = advertisingRepository.save(advertising);
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

        String oldFileName = null;
        String newFileName = null;

        try {
            // Delete old image
            String oldImageUrl = advertising.getImageUrl();
            if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                oldFileName = oldImageUrl.substring(oldImageUrl.lastIndexOf("/") + 1);
                fileStorageService.deleteFile(oldFileName, "advertising");
                System.out.println("✅ Old image deleted: " + oldFileName);
            }

            // Upload new image
            newFileName = fileStorageService.storeFile(imageFile, "advertising");
            String newImageUrl = "/api/files/advertising/" + newFileName;

            // Update entity
            advertising.setImageUrl(newImageUrl);
            Advertising updated = advertisingRepository.save(advertising);

            System.out.println("✅ Advertising image updated");
            return convertToDTO(updated);

        } catch (Exception e) {
            // Delete new image if error happens
            if (newFileName != null) {
                try {
                    fileStorageService.deleteFile(newFileName, "advertising");
                    System.out.println("🧹 Rolled back new image after update error: " + newFileName);
                } catch (Exception delEx) {
                    System.err.println("⚠️ Failed to rollback uploaded image: " + delEx.getMessage());
                }
            }
            throw new RuntimeException("Failed to update advertising image: " + e.getMessage(), e);
        }
    }

    /**
     * Delete advertising and its image file
     */
    @Transactional
    public void deleteAdvertising(Integer id) {
        System.out.println("🗑️ Deleting advertising with ID: " + id);

        Advertising advertising = advertisingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertising not found with id: " + id));

        String imageUrl = advertising.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                fileStorageService.deleteFile(filename, "advertising");
                System.out.println("✅ Image file deleted: " + filename);
            } catch (Exception e) {
                System.err.println("⚠️ Could not delete image file: " + e.getMessage());
            }
        }

        advertisingRepository.deleteById(id);
        System.out.println("✅ Advertising deleted from database");
    }

    /**
     * Convert entity to DTO
     */
    private AdvertisingDTO convertToDTO(Advertising advertising) {
        AdvertisingDTO dto = new AdvertisingDTO();
        dto.setId(advertising.getId());
        dto.setImageUrl(advertising.getImageUrl());
        dto.setCreateDate(advertising.getCreateDate());
        dto.setIsActive(advertising.getIsActive());
        return dto;
    }
}
