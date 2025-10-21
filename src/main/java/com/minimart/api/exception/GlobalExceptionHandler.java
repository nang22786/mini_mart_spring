package com.minimart.api.exception;

import com.minimart.api.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        ApiResponse<Void> response = new ApiResponse<>(false, ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(DuplicateResourceException ex) {
        ApiResponse<Void> response = new ApiResponse<>(false, ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(InvalidRequestException ex) {
        ApiResponse<Void> response = new ApiResponse<>(false, ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        ApiResponse<Void> response = new ApiResponse<>(false, "An error occurred: " + ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}