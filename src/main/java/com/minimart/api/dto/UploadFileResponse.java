package com.minimart.api.dto;

public class UploadFileResponse {
    
    private boolean success;
    private String message;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private long fileSize;

    // Constructors
    public UploadFileResponse() {
    }

    public UploadFileResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public UploadFileResponse(boolean success, String message, String fileName, 
                             String fileUrl, String fileType, long fileSize) {
        this.success = success;
        this.message = message;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.fileSize = fileSize;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}