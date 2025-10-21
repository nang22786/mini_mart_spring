package com.minimart.api.controller;

import com.minimart.api.model.Category;
import com.minimart.api.service.CategoryService;
import com.minimart.api.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    // Get all categories
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCategories(
            @RequestParam(required = false) String sort) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Category> categories;
            
            if ("name".equalsIgnoreCase(sort)) {
                categories = categoryService.getAllCategoriesOrderedByName();
            } else if ("date".equalsIgnoreCase(sort)) {
                categories = categoryService.getAllCategoriesOrderedByDate();
            } else {
                categories = categoryService.getAllCategories();
            }
            
            response.put("success", true);
            response.put("data", categories);
            response.put("count", categories.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error retrieving categories: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Get category by ID
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCategoryById(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Category category = categoryService.getCategoryById(id)
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
            
            response.put("success", true);
            response.put("data", category);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error retrieving category: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Create category (with optional image)
    @PostMapping
    public ResponseEntity<Map<String, Object>> createCategory(
            @RequestParam("name") String name,
            @RequestParam(value = "image", required = false) MultipartFile imageFile) {
        
        Map<String, Object> response = new HashMap<>();
        String uploadedFilename = null;
        
        try {
            System.out.println("📝 Creating category");
            System.out.println("   Category name: " + name);
            System.out.println("   Has image: " + (imageFile != null && !imageFile.isEmpty()));
            
            // Validate input
            if (name == null || name.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Category name is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Check if category name already exists
            if (categoryService.categoryNameExists(name)) {
                response.put("success", false);
                response.put("message", "Category with name '" + name + "' already exists");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
            
            // Create category object
            Category category = new Category();
            category.setName(name);
            
            // Handle optional image upload
            if (imageFile != null && !imageFile.isEmpty()) {
                System.out.println("📤 Uploading image file...");
                
                uploadedFilename = fileStorageService.storeFile(imageFile, "category");
                
                // Generate RELATIVE URL (without localhost)
                String imageUrl = "/api/files/category/" + uploadedFilename;
                
                category.setImage(imageUrl);
                System.out.println("✅ Image uploaded: " + imageUrl);
            } else {
                System.out.println("ℹ️ No image uploaded - category created without image");
            }
            
            Category newCategory = categoryService.createCategory(category);
            
            response.put("success", true);
            response.put("message", "Category created successfully");
            response.put("data", newCategory);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (RuntimeException e) {
            System.err.println("❌ Error: " + e.getMessage());
            
            // Rollback: delete uploaded image if exists
            if (uploadedFilename != null) {
                try {
                    fileStorageService.deleteFile(uploadedFilename, "category");
                    System.out.println("🗑️ Rolled back uploaded image: " + uploadedFilename);
                } catch (Exception ex) {
                    System.err.println("⚠️ Failed to delete uploaded image: " + ex.getMessage());
                }
            }
            
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            System.err.println("❌ Error creating category: " + e.getMessage());
            e.printStackTrace();
            
            // Rollback: delete uploaded image if exists
            if (uploadedFilename != null) {
                try {
                    fileStorageService.deleteFile(uploadedFilename, "category");
                    System.out.println("🗑️ Rolled back uploaded image: " + uploadedFilename);
                } catch (Exception ex) {
                    System.err.println("⚠️ Failed to delete uploaded image: " + ex.getMessage());
                }
            }
            
            response.put("success", false);
            response.put("message", "Error creating category: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Update category (with optional image)
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCategory(
            @PathVariable Integer id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "image", required = false) MultipartFile imageFile) {
        
        Map<String, Object> response = new HashMap<>();
        String uploadedFilename = null;
        String oldImageUrl = null;
        
        try {
            System.out.println("📝 Updating category");
            System.out.println("   Category ID: " + id);
            System.out.println("   New name: " + name);
            System.out.println("   Has new image: " + (imageFile != null && !imageFile.isEmpty()));
            
            // Get existing category
            Category existingCategory = categoryService.getCategoryById(id)
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
            
            // Validate input - at least one field must be provided
            if ((name == null || name.trim().isEmpty()) && (imageFile == null || imageFile.isEmpty())) {
                response.put("success", false);
                response.put("message", "At least one field (name or image) must be provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Check if new name conflicts with existing category (excluding current one)
            if (name != null && !name.trim().isEmpty() && !existingCategory.getName().equalsIgnoreCase(name)) {
                if (categoryService.categoryNameExists(name)) {
                    response.put("success", false);
                    response.put("message", "Category with name '" + name + "' already exists");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                }
            }
            
            // Store old image URL for potential cleanup
            oldImageUrl = existingCategory.getImage();
            
            // Prepare update
            Category categoryDetails = new Category();
            
            // Use new name if provided, otherwise keep existing
            if (name != null && !name.trim().isEmpty()) {
                categoryDetails.setName(name);
            } else {
                categoryDetails.setName(existingCategory.getName());
            }
            
            // Handle optional image upload
            if (imageFile != null && !imageFile.isEmpty()) {
                System.out.println("📤 Uploading new image...");
                
                uploadedFilename = fileStorageService.storeFile(imageFile, "category");
                
                // Generate RELATIVE URL (without localhost)
                String imageUrl = "/api/files/category/" + uploadedFilename;
                
                categoryDetails.setImage(imageUrl);
                System.out.println("✅ New image uploaded: " + imageUrl);
            } else {
                // Keep existing image
                categoryDetails.setImage(existingCategory.getImage());
                System.out.println("ℹ️ Keeping existing image");
            }
            
            Category updatedCategory = categoryService.updateCategory(id, categoryDetails);
            
            // Delete old image only after successful update and if new image was uploaded
            if (uploadedFilename != null && oldImageUrl != null && !oldImageUrl.isEmpty()) {
                String oldFilename = extractFilenameFromUrl(oldImageUrl);
                if (oldFilename != null && !oldFilename.isEmpty()) {
                    try {
                        fileStorageService.deleteFile(oldFilename, "category");
                        System.out.println("🗑️ Deleted old image: " + oldFilename);
                    } catch (Exception e) {
                        System.err.println("⚠️ Failed to delete old image: " + e.getMessage());
                    }
                }
            }
            
            response.put("success", true);
            response.put("message", "Category updated successfully");
            response.put("data", updatedCategory);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            System.err.println("❌ Error: " + e.getMessage());
            
            // Rollback: delete new uploaded image if exists
            if (uploadedFilename != null) {
                try {
                    fileStorageService.deleteFile(uploadedFilename, "category");
                    System.out.println("🗑️ Rolled back uploaded image: " + uploadedFilename);
                } catch (Exception ex) {
                    System.err.println("⚠️ Failed to delete uploaded image: " + ex.getMessage());
                }
            }
            
            response.put("success", false);
            response.put("message", e.getMessage());
            
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
        } catch (Exception e) {
            System.err.println("❌ Error updating category: " + e.getMessage());
            e.printStackTrace();
            
            // Rollback: delete new uploaded image if exists
            if (uploadedFilename != null) {
                try {
                    fileStorageService.deleteFile(uploadedFilename, "category");
                    System.out.println("🗑️ Rolled back uploaded image: " + uploadedFilename);
                } catch (Exception ex) {
                    System.err.println("⚠️ Failed to delete uploaded image: " + ex.getMessage());
                }
            }
            
            response.put("success", false);
            response.put("message", "Error updating category: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Delete category
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCategory(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get category to delete its image
            Category category = categoryService.getCategoryById(id)
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
            
            // Delete category image if exists
            if (category.getImage() != null && !category.getImage().isEmpty()) {
                String filename = extractFilenameFromUrl(category.getImage());
                
                if (filename != null && !filename.isEmpty()) {
                    try {
                        fileStorageService.deleteFile(filename, "category");
                        System.out.println("🗑️ Deleted category image: " + filename);
                    } catch (Exception e) {
                        System.err.println("⚠️ Failed to delete image: " + e.getMessage());
                    }
                }
            }
            
            categoryService.deleteCategory(id);
            
            response.put("success", true);
            response.put("message", "Category deleted successfully");
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting category: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Get category count
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getCategoryCount() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            long count = categoryService.getCategoryCount();
            
            response.put("success", true);
            response.put("count", count);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error getting category count: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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