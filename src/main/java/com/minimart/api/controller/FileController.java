package com.minimart.api.controller;

import com.minimart.api.dto.UploadFileResponse;
import com.minimart.api.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Upload file endpoint - Returns Cloudinary URL
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadFileResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", required = false) String type) {

        System.out.println("=== FILE UPLOAD REQUEST ===");
        System.out.println("File: " + file.getOriginalFilename());
        System.out.println("Type: " + (type != null ? type : "default"));
        System.out.println("Size: " + file.getSize() + " bytes");

        try {
            // Upload to Cloudinary - returns full URL
            String cloudinaryUrl = fileStorageService.storeFile(file, type);

            System.out.println("✅ File uploaded to Cloudinary: " + cloudinaryUrl);

            UploadFileResponse response = new UploadFileResponse(
                    true,
                    "File uploaded successfully",
                    cloudinaryUrl, // Full Cloudinary URL (not just filename)
                    cloudinaryUrl, // Same URL for download
                    file.getContentType(),
                    file.getSize());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Upload failed: " + e.getMessage());
            e.printStackTrace();

            UploadFileResponse response = new UploadFileResponse(
                    false,
                    "Failed to upload file: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Upload file to category folder - Returns Cloudinary URL
     */
    @PostMapping("/upload/category")
    public ResponseEntity<UploadFileResponse> uploadFileToCategory(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category) {

        System.out.println("=== CATEGORY FILE UPLOAD REQUEST ===");
        System.out.println("File: " + file.getOriginalFilename());
        System.out.println("Category: " + category);
        System.out.println("Size: " + file.getSize() + " bytes");

        try {
            // Upload to Cloudinary category folder - returns full URL
            String cloudinaryUrl = fileStorageService.storeFileInCategoryFolder(file, category);

            System.out.println("✅ File uploaded to Cloudinary: " + cloudinaryUrl);

            UploadFileResponse response = new UploadFileResponse(
                    true,
                    "File uploaded successfully",
                    cloudinaryUrl, // Full Cloudinary URL
                    cloudinaryUrl, // Same URL for download
                    file.getContentType(),
                    file.getSize());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Upload failed: " + e.getMessage());
            e.printStackTrace();

            UploadFileResponse response = new UploadFileResponse(
                    false,
                    "Failed to upload file: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Delete file from Cloudinary
     * Accepts full Cloudinary URL or just the public_id
     */
    @DeleteMapping("/delete")
    public ResponseEntity<UploadFileResponse> deleteFile(
            @RequestParam("url") String fileUrl) {

        System.out.println("=== FILE DELETE REQUEST ===");
        System.out.println("URL: " + fileUrl);

        try {
            fileStorageService.deleteFile(fileUrl);
            System.out.println("✅ File deleted from Cloudinary");

            return ResponseEntity.ok(new UploadFileResponse(true, "File deleted successfully"));

        } catch (Exception e) {
            System.err.println("❌ Delete failed: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UploadFileResponse(false, "Failed to delete file: " + e.getMessage()));
        }
    }

    /**
     * DEPRECATED: Legacy endpoint for backwards compatibility
     * Now returns error message explaining migration to Cloudinary
     */
    @GetMapping("/{folder}/{filename:.+}")
    public ResponseEntity<String> legacyDownloadFile(
            @PathVariable String folder,
            @PathVariable String filename) {

        System.out.println("⚠️ Legacy endpoint called: /" + folder + "/" + filename);

        return ResponseEntity.status(HttpStatus.GONE)
                .body("File serving has been migrated to Cloudinary. " +
                        "Please use the Cloudinary URL returned from the upload endpoint. " +
                        "If you need to re-upload, use POST /api/files/upload");
    }

    /**
     * DEPRECATED: Legacy endpoint for backwards compatibility
     */
    @GetMapping("/products/{category}/{filename:.+}")
    public ResponseEntity<String> legacyDownloadProductFile(
            @PathVariable String category,
            @PathVariable String filename) {

        System.out.println("⚠️ Legacy product endpoint called: /products/" + category + "/" + filename);

        return ResponseEntity.status(HttpStatus.GONE)
                .body("File serving has been migrated to Cloudinary. " +
                        "Please use the Cloudinary URL returned from the upload endpoint. " +
                        "If you need to re-upload, use POST /api/files/upload/category");
    }
}