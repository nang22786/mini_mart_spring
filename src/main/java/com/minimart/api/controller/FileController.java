package com.minimart.api.controller;

import com.minimart.api.dto.UploadFileResponse;
import com.minimart.api.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Upload file endpoint
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
            String fileName = fileStorageService.storeFile(file, type);

            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/files/")
                    .path(type != null ? type + "/" : "")
                    .path(fileName)
                    .toUriString();

            System.out.println("✅ File uploaded successfully: " + fileName);

            UploadFileResponse response = new UploadFileResponse(
                    true,
                    "File uploaded successfully",
                    fileName,
                    fileDownloadUri,
                    file.getContentType(),
                    file.getSize()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Upload failed: " + e.getMessage());
            e.printStackTrace();

            UploadFileResponse response = new UploadFileResponse(
                    false,
                    "Failed to upload file: " + e.getMessage()
            );

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * Download/View file from products folder
     * Handles: /api/files/products/{category}/{filename}
     * Example: /api/files/products/Beverages/image.jpg
     */
    @GetMapping(value = "/products/{category}/{filename:.+}", produces = "application/octet-stream")
    public void downloadProductFile(
            @PathVariable String category,
            @PathVariable String filename,
            HttpServletResponse response) throws IOException {
        
        System.out.println("=== PRODUCT FILE DOWNLOAD REQUEST ===");
        System.out.println("Category: " + category);
        System.out.println("File: " + filename);
        
        try {
            // Path: uploads/products/{category}/{filename}
            Path filePath = Paths.get("uploads/products")
                    .toAbsolutePath()
                    .normalize()
                    .resolve(category)
                    .resolve(filename);
            
            System.out.println("📂 Checking path: " + filePath);
            
            if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                System.err.println("❌ File not found");
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }
            
            String contentType = getContentType(filename);
            long fileSize = Files.size(filePath);
            
            System.out.println("✅ File found!");
            System.out.println("   Content-Type: " + contentType);
            System.out.println("   Size: " + fileSize + " bytes");
            
            // Set response headers
            response.setContentType(contentType);
            response.setContentLengthLong(fileSize);
            response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
            response.setHeader("Cache-Control", "max-age=31536000");
            
            // Stream file directly to response
            try (InputStream is = new FileInputStream(filePath.toFile());
                 OutputStream os = response.getOutputStream()) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
            
            System.out.println("✅ File sent successfully");
            
        } catch (IOException e) {
            System.err.println("❌ Error serving file: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving file");
        }
    }
    
    /**
     * Download/View file from folder
     * Handles: /api/files/{folder}/{filename}
     * Example: /api/files/profile/user.jpg, /api/files/category/image.jpg
     */
    @GetMapping(value = "/{folder}/{filename:.+}", produces = "application/octet-stream")
    public void downloadFile(
            @PathVariable String folder,
            @PathVariable String filename,
            HttpServletResponse response) throws IOException {
        
        System.out.println("=== FILE DOWNLOAD REQUEST ===");
        System.out.println("Folder: " + folder);
        System.out.println("File: " + filename);
        
        try {
            // Try: uploads/{folder}/{filename}
            Path filePath = fileStorageService.getFileStorageLocation(null)
                    .resolve(folder)
                    .resolve(filename)
                    .normalize();
            
            System.out.println("📂 Checking path: " + filePath);
            
            if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                // Try: uploads/{type}/{filename}
                System.out.println("📂 Not found, trying as type folder...");
                filePath = fileStorageService.getFileStorageLocation(folder)
                        .resolve(filename)
                        .normalize();
                System.out.println("📂 Checking path: " + filePath);
            }
            
            if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                System.err.println("❌ File not found");
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }
            
            String contentType = getContentType(filename);
            long fileSize = Files.size(filePath);
            
            System.out.println("✅ File found!");
            System.out.println("   Content-Type: " + contentType);
            System.out.println("   Size: " + fileSize + " bytes");
            
            // Set response headers
            response.setContentType(contentType);
            response.setContentLengthLong(fileSize);
            response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
            response.setHeader("Cache-Control", "max-age=31536000");
            
            // Stream file directly to response
            try (InputStream is = new FileInputStream(filePath.toFile());
                 OutputStream os = response.getOutputStream()) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
            
            System.out.println("✅ File sent successfully");
            
        } catch (IOException e) {
            System.err.println("❌ Error serving file: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving file");
        }
    }
    
    /**
     * Determine content type from file extension
     */
    private String getContentType(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "application/octet-stream";
        }
        
        String extension = "";
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            extension = filename.substring(lastDotIndex + 1).toLowerCase();
        }
        
        switch (extension) {
            // Images
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";
            case "bmp":
                return "image/bmp";
            case "ico":
                return "image/x-icon";
            
            // Documents
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt":
                return "text/plain";
            case "csv":
                return "text/csv";
            
            // Video
            case "mp4":
                return "video/mp4";
            case "avi":
                return "video/x-msvideo";
            case "mov":
                return "video/quicktime";
            case "webm":
                return "video/webm";
            
            // Audio
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "ogg":
                return "audio/ogg";
            
            // Archives
            case "zip":
                return "application/zip";
            case "rar":
                return "application/x-rar-compressed";
            case "7z":
                return "application/x-7z-compressed";
            
            // Web
            case "html":
            case "htm":
                return "text/html";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";
            case "json":
                return "application/json";
            case "xml":
                return "application/xml";
            
            default:
                return "application/octet-stream";
        }
    }

    /**
     * Delete file from products folder
     */
    @DeleteMapping("/products/{category}/{filename:.+}")
    public ResponseEntity<UploadFileResponse> deleteProductFile(
            @PathVariable String category,
            @PathVariable String filename) {
        
        System.out.println("=== PRODUCT FILE DELETE REQUEST ===");
        System.out.println("Category: " + category);
        System.out.println("File: " + filename);

        try {
            fileStorageService.deleteFileInCategoryFolder(filename, category);
            System.out.println("✅ Product file deleted");
            
            return ResponseEntity.ok(new UploadFileResponse(true, "File deleted successfully"));

        } catch (Exception e) {
            System.err.println("❌ Delete failed: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UploadFileResponse(false, "Failed to delete file: " + e.getMessage()));
        }
    }

    /**
     * Delete file from other folders
     */
    @DeleteMapping("/{folder}/{filename:.+}")
    public ResponseEntity<UploadFileResponse> deleteFile(
            @PathVariable String folder,
            @PathVariable String filename) {
        
        System.out.println("=== FILE DELETE REQUEST ===");
        System.out.println("Folder: " + folder);
        System.out.println("File: " + filename);

        try {
            // Try deleting from type folder
            fileStorageService.deleteFile(filename, folder);
            System.out.println("✅ File deleted");

            return ResponseEntity.ok(new UploadFileResponse(true, "File deleted successfully"));

        } catch (Exception e) {
            System.err.println("❌ Delete failed: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UploadFileResponse(false, "Failed to delete file: " + e.getMessage()));
        }
    }
}