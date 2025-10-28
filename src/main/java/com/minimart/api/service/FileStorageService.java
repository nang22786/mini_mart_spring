package com.minimart.api.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.minimart.api.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {

    @Autowired
    private Cloudinary cloudinary;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    /**
     * Store uploaded file in default directory
     */
    public String storeFile(MultipartFile file) {
        return storeFile(file, null);
    }

    /**
     * Store uploaded file in specified directory type (Cloudinary folder)
     * 
     * @param file          - the file to upload
     * @param directoryType - "profile", "category", "product", "payment",
     *                      "advertising" or null for default
     * @return Full Cloudinary URL
     */
    public String storeFile(MultipartFile file, String directoryType) {
        System.out.println("📥 Received file upload request");
        System.out.println("   Directory type: " + (directoryType != null ? directoryType : "default"));
        System.out.println("   Original filename: " + file.getOriginalFilename());
        System.out.println("   Content type: " + file.getContentType());
        System.out.println("   Size: " + file.getSize() + " bytes");

        // Validate file
        validateFile(file);

        // Determine Cloudinary folder name
        String folderName = getFolderName(directoryType);

        // Generate unique filename
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String uniqueFileName = UUID.randomUUID().toString();

        try {
            // Upload to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folderName,
                            "public_id", uniqueFileName,
                            "resource_type", "auto"));

            String imageUrl = uploadResult.get("secure_url").toString();
            System.out.println("✅ File uploaded to Cloudinary: " + imageUrl);

            return imageUrl;

        } catch (IOException ex) {
            throw new FileStorageException("Could not upload file to Cloudinary: " + ex.getMessage(), ex);
        }
    }

    /**
     * Store uploaded file in category subfolder (for products)
     * Saves to Cloudinary: products/{categoryName}/{filename}
     * 
     * @param file         - the file to upload
     * @param categoryName - category name to create subfolder
     * @return Full Cloudinary URL
     */
    public String storeFileInCategoryFolder(MultipartFile file, String categoryName) {
        System.out.println("📥 Received file upload request for category folder");
        System.out.println("   Category: " + categoryName);
        System.out.println("   Original filename: " + file.getOriginalFilename());
        System.out.println("   Content type: " + file.getContentType());
        System.out.println("   Size: " + file.getSize() + " bytes");

        // Validate file
        validateFile(file);

        // Sanitize category name for folder
        String sanitizedCategoryName = sanitizeFolderName(categoryName);

        // Create Cloudinary folder path: products/{categoryName}
        String folderPath = "products/" + sanitizedCategoryName;

        // Generate unique filename
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String uniqueFileName = UUID.randomUUID().toString();

        try {
            // Upload to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folderPath,
                            "public_id", uniqueFileName,
                            "resource_type", "auto"));

            String imageUrl = uploadResult.get("secure_url").toString();
            System.out.println("✅ File uploaded to Cloudinary category folder: " + imageUrl);

            return imageUrl;

        } catch (IOException ex) {
            throw new FileStorageException("Could not upload file to Cloudinary: " + ex.getMessage(), ex);
        }
    }

    /**
     * Delete file from Cloudinary (default directory)
     */
    public void deleteFile(String fileUrl) {
        deleteFile(fileUrl, null);
    }

    /**
     * Delete file from Cloudinary
     * 
     * @param fileUrl       - Full Cloudinary URL or public_id
     * @param directoryType - folder type (optional, used for extracting public_id)
     */
    public void deleteFile(String fileUrl, String directoryType) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            System.out.println("⚠️ No file URL provided for deletion");
            return;
        }

        try {
            String publicId = extractPublicId(fileUrl);

            if (publicId != null) {
                Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                System.out.println("✅ File deleted from Cloudinary: " + publicId);
                System.out.println("   Result: " + result.get("result"));
            } else {
                System.out.println("⚠️ Could not extract public_id from URL: " + fileUrl);
            }
        } catch (Exception ex) {
            System.err.println("❌ Error deleting file from Cloudinary: " + ex.getMessage());
        }
    }

    /**
     * Delete file from category subfolder in Cloudinary
     * 
     * @param fileUrl      - Full Cloudinary URL
     * @param categoryName - category name (not used but kept for compatibility)
     */
    public void deleteFileInCategoryFolder(String fileUrl, String categoryName) {
        // Cloudinary doesn't need category name to delete, just the URL
        deleteFile(fileUrl, null);
    }

    /**
     * Get folder name based on directory type
     */
    private String getFolderName(String directoryType) {
        if (directoryType == null || directoryType.isEmpty()) {
            return "uploads";
        }

        switch (directoryType.toLowerCase()) {
            case "profile":
                return "profile";
            case "category":
                return "category";
            case "product":
                return "products";
            case "payment":
                return "payments";
            case "advertising":
                return "advertising";
            default:
                return "uploads";
        }
    }

    /**
     * Extract public_id from Cloudinary URL
     * Example URL:
     * https://res.cloudinary.com/daovbs2bm/image/upload/v1234567/profile/uuid-123.jpg
     * Returns: profile/uuid-123
     */
    private String extractPublicId(String imageUrl) {
        try {
            // Check if it's a Cloudinary URL
            if (!imageUrl.contains("cloudinary.com")) {
                return null;
            }

            // Split by /upload/ to get the path after upload
            String[] parts = imageUrl.split("/upload/");
            if (parts.length < 2) {
                return null;
            }

            // Remove version number (v1234567/)
            String pathWithVersion = parts[1];
            String path = pathWithVersion.replaceFirst("v\\d+/", "");

            // Remove file extension
            int lastDotIndex = path.lastIndexOf('.');
            if (lastDotIndex > 0) {
                return path.substring(0, lastDotIndex);
            }

            return path;
        } catch (Exception e) {
            System.err.println("❌ Error extracting public_id: " + e.getMessage());
            return null;
        }
    }

    /**
     * Sanitize folder name (replace special characters)
     */
    private String sanitizeFolderName(String name) {
        if (name == null) {
            return "default";
        }
        // Replace special characters with underscore, keep alphanumeric and spaces
        return name.replaceAll("[^a-zA-Z0-9\\s]", "_").trim();
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new FileStorageException("Cannot upload empty file");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException("File size exceeds maximum limit of 5MB");
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new FileStorageException("Only image files are allowed");
        }

        // Check file extension
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new FileStorageException("Invalid file name");
        }

        String extension = getFileExtension(originalFileName);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new FileStorageException("File type not allowed. Allowed types: " + ALLOWED_EXTENSIONS);
        }
    }

    /**
     * Extract file extension
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}