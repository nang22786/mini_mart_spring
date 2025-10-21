package com.minimart.api.service;

import com.minimart.api.config.FileStorageProperties;
import com.minimart.api.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;
    
    // Additional directory paths
    @Value("${file.upload.profile:uploads/profile}")
    private String profileUploadDir;
    
    @Value("${file.upload.category:uploads/category}")
    private String categoryUploadDir;
    
    @Value("${file.upload.product:uploads/products}")
    private String productUploadDir;
    
    @Value("${file.upload.payment:uploads/payments}")  // ✅ FIXED!
    private String paymentUploadDir;
    
    @Value("${file.upload.advertising:uploads/advertising}")
    private String advertisingUploadDir;
    
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Autowired
    public FileStorageService(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
            System.out.println("✅ File upload directory created: " + this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create upload directory!", ex);
        }
    }

    /**
     * Store uploaded file in default directory
     */
    public String storeFile(MultipartFile file) {
        return storeFile(file, null);
    }
    
    /**
     * Store uploaded file in specified directory type
     * @param file - the file to upload
     * @param directoryType - "profile", "category", "product", "payment", "banner" or null for default
     */
    public String storeFile(MultipartFile file, String directoryType) {
        System.out.println("📥 Received file upload request");
        System.out.println("   Directory type: " + (directoryType != null ? directoryType : "default"));
        System.out.println("   Original filename: " + file.getOriginalFilename());
        System.out.println("   Content type: " + file.getContentType());
        System.out.println("   Size: " + file.getSize() + " bytes");

        // Validate file
        validateFile(file);

        // Determine upload directory
        Path uploadPath = getUploadPath(directoryType);
        
        // Create directory if not exists
        try {
            Files.createDirectories(uploadPath);
            System.out.println("✅ Directory ensured: " + uploadPath);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create upload directory: " + uploadPath, ex);
        }

        // Generate unique filename
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String newFileName = UUID.randomUUID().toString() + "." + fileExtension;

        try {
            // Check if filename contains invalid characters
            if (originalFileName.contains("..")) {
                throw new FileStorageException("Invalid file path: " + originalFileName);
            }

            // Copy file to storage location
            Path targetLocation = uploadPath.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("✅ File stored successfully: " + targetLocation);
            return newFileName;

        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + newFileName, ex);
        }
    }

    /**
     * Store uploaded file in category subfolder (for products)
     * Saves to: uploads/products/{categoryName}/{filename}
     * @param file - the file to upload
     * @param categoryName - category name to create subfolder
     * @return filename
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
        
        // ✅ Create path: uploads/products/{categoryName}/
        Path uploadPath = Paths.get(productUploadDir, sanitizedCategoryName)
                .toAbsolutePath()
                .normalize();
        
        // Create directory if not exists
        try {
            Files.createDirectories(uploadPath);
            System.out.println("✅ Category folder created: " + uploadPath);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create category upload directory: " + uploadPath, ex);
        }

        // Generate unique filename
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String newFileName = UUID.randomUUID().toString() + "." + fileExtension;

        try {
            // Check if filename contains invalid characters
            if (originalFileName.contains("..")) {
                throw new FileStorageException("Invalid file path: " + originalFileName);
            }

            // Copy file to storage location
            Path targetLocation = uploadPath.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("✅ File stored successfully in category folder: " + targetLocation);
            return newFileName;

        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + newFileName, ex);
        }
    }

    /**
     * Delete file from default directory
     */
    public void deleteFile(String fileName) {
        deleteFile(fileName, null);
    }
    
    /**
     * Delete file from specified directory type
     */
    public void deleteFile(String fileName, String directoryType) {
        try {
            Path uploadPath = getUploadPath(directoryType);
            Path filePath = uploadPath.resolve(fileName).normalize();
            boolean deleted = Files.deleteIfExists(filePath);
            
            if (deleted) {
                System.out.println("✅ File deleted: " + filePath);
            } else {
                System.out.println("⚠️ File not found: " + filePath);
            }
        } catch (IOException ex) {
            System.err.println("❌ Error deleting file " + fileName + ": " + ex.getMessage());
        }
    }

    /**
     * Delete file from category subfolder (for products)
     * Deletes from: uploads/products/{categoryName}/{filename}
     * @param fileName - the filename to delete
     * @param categoryName - category name subfolder
     */
    public void deleteFileInCategoryFolder(String fileName, String categoryName) {
        try {
            String sanitizedCategoryName = sanitizeFolderName(categoryName);
            
            // ✅ Path: uploads/products/{categoryName}/
            Path uploadPath = Paths.get(productUploadDir, sanitizedCategoryName)
                    .toAbsolutePath()
                    .normalize();
                    
            Path filePath = uploadPath.resolve(fileName).normalize();
            boolean deleted = Files.deleteIfExists(filePath);
            
            if (deleted) {
                System.out.println("✅ File deleted from category folder: " + filePath);
                
                // Try to delete empty category folder
                try {
                    if (Files.list(uploadPath).findAny().isEmpty()) {
                        Files.delete(uploadPath);
                        System.out.println("✅ Empty category folder deleted: " + uploadPath);
                    }
                } catch (IOException e) {
                    // Ignore if folder is not empty or cannot be deleted
                }
            } else {
                System.out.println("⚠️ File not found in category folder: " + filePath);
            }
        } catch (IOException ex) {
            System.err.println("❌ Error deleting file " + fileName + " from category folder: " + ex.getMessage());
        }
    }

    /**
     * Get file storage location for specific directory type
     */
    public Path getFileStorageLocation(String directoryType) {
        return getUploadPath(directoryType);
    }
    
    /**
     * Get default file storage location
     */
    public Path getFileStorageLocation() {
        return fileStorageLocation;
    }
    
    /**
     * Get upload path based on directory type
     */
    private Path getUploadPath(String directoryType) {
        if (directoryType == null || directoryType.isEmpty()) {
            return fileStorageLocation;
        }
        
        String uploadDir;
        switch (directoryType.toLowerCase()) {
            case "profile":
                uploadDir = profileUploadDir;
                break;
            case "category":
                uploadDir = categoryUploadDir;
                break;
            case "product":
                uploadDir = productUploadDir;
                break;
            case "payment":  // ✅ FIXED!
                uploadDir = paymentUploadDir;  // ✅ FIXED!
                break;
            case "advertising":
                uploadDir = advertisingUploadDir;
                break;
            default:
                return fileStorageLocation;
        }
        
        return Paths.get(uploadDir).toAbsolutePath().normalize();
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