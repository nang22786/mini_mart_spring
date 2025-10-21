package com.minimart.api.controller;

import com.minimart.api.dto.AdvertisingDTO;
import com.minimart.api.service.AdvertisingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/advertising")
@CrossOrigin(origins = "*")
public class AdvertisingController {
    
    @Autowired
    private AdvertisingService advertisingService;
    
    /**
     * Create new advertising with image
     * POST /api/advertising
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> createAdvertising(
            @RequestParam("image") MultipartFile imageFile) {
        
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("=== CREATE ADVERTISING REQUEST ===");
        System.out.println("File: " + imageFile.getOriginalFilename());
        System.out.println("Size: " + imageFile.getSize() + " bytes");
        
        try {
            if (imageFile.isEmpty()) {
                response.put("success", false);
                response.put("message", "Image file is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            AdvertisingDTO created = advertisingService.createAdvertising(imageFile);
            
            response.put("success", true);
            response.put("message", "Advertising created successfully");
            response.put("data", created);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
                    
        } catch (Exception e) {
            System.err.println("❌ Create failed: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("message", "Error creating advertising: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get all advertising (sorted by newest first)
     * GET /api/advertising
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllAdvertising() {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("=== GET ALL ADVERTISING REQUEST ===");
        
        try {
            List<AdvertisingDTO> advertisingList = advertisingService.getAllAdvertising();
            
            response.put("success", true);
            response.put("data", advertisingList);
            response.put("count", advertisingList.size());
            return ResponseEntity.ok(response);
                    
        } catch (Exception e) {
            System.err.println("❌ Fetch failed: " + e.getMessage());
            
            response.put("success", false);
            response.put("message", "Error retrieving advertising: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get advertising by ID
     * GET /api/advertising/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAdvertisingById(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("=== GET ADVERTISING BY ID REQUEST ===");
        System.out.println("ID: " + id);
        
        try {
            AdvertisingDTO advertising = advertisingService.getAdvertisingById(id);
            
            response.put("success", true);
            response.put("data", advertising);
            return ResponseEntity.ok(response);
                    
        } catch (RuntimeException e) {
            System.err.println("❌ Not found: " + e.getMessage());
            
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            
            response.put("success", false);
            response.put("message", "Error retrieving advertising: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Update advertising image
     * PUT /api/advertising/{id}
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> updateAdvertisingImage(
            @PathVariable Integer id,
            @RequestParam("image") MultipartFile imageFile) {
        
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("=== UPDATE ADVERTISING IMAGE REQUEST ===");
        System.out.println("ID: " + id);
        System.out.println("File: " + imageFile.getOriginalFilename());
        
        try {
            if (imageFile.isEmpty()) {
                response.put("success", false);
                response.put("message", "Image file is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            AdvertisingDTO updated = advertisingService.updateAdvertisingImage(id, imageFile);
            
            response.put("success", true);
            response.put("message", "Advertising image updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
                    
        } catch (RuntimeException e) {
            System.err.println("❌ Not found: " + e.getMessage());
            
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                    
        } catch (Exception e) {
            System.err.println("❌ Update failed: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("message", "Error updating advertising: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Delete advertising
     * DELETE /api/advertising/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteAdvertising(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("=== DELETE ADVERTISING REQUEST ===");
        System.out.println("ID: " + id);
        
        try {
            advertisingService.deleteAdvertising(id);
            
            response.put("success", true);
            response.put("message", "Advertising deleted successfully");
            return ResponseEntity.ok(response);
                    
        } catch (RuntimeException e) {
            System.err.println("❌ Not found: " + e.getMessage());
            
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                    
        } catch (Exception e) {
            System.err.println("❌ Delete failed: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("message", "Error deleting advertising: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> toggleAdvertisingStatus(
            @PathVariable Integer id,
            @RequestParam("isActive") Boolean isActive) {

        Map<String, Object> response = new HashMap<>();
        try {
            AdvertisingDTO updated = advertisingService.toggleAdvertisingStatus(id, isActive);
            response.put("success", true);
            response.put("message", "Advertising status updated");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
