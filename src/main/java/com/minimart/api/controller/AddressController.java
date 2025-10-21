package com.minimart.api.controller;

import com.minimart.api.model.Address;
import com.minimart.api.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/addresses")
@CrossOrigin(origins = "*")
public class AddressController {
    
    @Autowired
    private AddressService addressService;
    
    // GET ALL addresses (for admin/owner)
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllAddresses() {
        List<Address> addresses = addressService.getAllAddresses();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "All addresses retrieved successfully");
        response.put("count", addresses.size());
        response.put("data", addresses);
        return ResponseEntity.ok(response);
    }
    
    // GET address by ID
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAddressById(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        
        return addressService.getAddressById(id)
                .map(address -> {
                    response.put("success", true);
                    response.put("message", "Address found");
                    response.put("data", address);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    response.put("success", false);
                    response.put("message", "Address not found with id: " + id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                });
    }
    
    // GET all addresses for a specific user
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getAllAddressesByUser(@PathVariable Long userId) {
        List<Address> addresses = addressService.getAllAddressesByUserId(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Addresses retrieved successfully");
        response.put("count", addresses.size());
        response.put("data", addresses);
        return ResponseEntity.ok(response);
    }
    
    // POST - Create new address
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAddress(@Valid @RequestBody Address address) {
        Map<String, Object> response = new HashMap<>();
        
        // Manual validation for user_id during CREATE
        if (address.getUser_id() == null) {
            response.put("success", false);
            response.put("message", "User ID is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        Address createdAddress = addressService.createAddress(address);
        response.put("success", true);
        response.put("message", "Address created successfully");
        response.put("data", createdAddress);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    // PUT - Update address by ID only
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateAddress(
            @PathVariable Integer id,
            @Valid @RequestBody Address addressDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Address updatedAddress = addressService.updateAddressById(id, addressDetails);
            response.put("success", true);
            response.put("message", "Address updated successfully");
            response.put("data", updatedAddress);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    // DELETE address by ID only
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteAddress(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            addressService.deleteAddressById(id);
            response.put("success", true);
            response.put("message", "Address deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    // GET addresses by user and province
    @GetMapping("/user/{userId}/province/{province}")
    public ResponseEntity<Map<String, Object>> getAddressesByUserAndProvince(
            @PathVariable Long userId,
            @PathVariable String province) {
        
        List<Address> addresses = addressService.getAddressesByUserIdAndProvince(userId, province);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Addresses retrieved successfully");
        response.put("count", addresses.size());
        response.put("data", addresses);
        return ResponseEntity.ok(response);
    }
    
    // GET addresses by user and district
    @GetMapping("/user/{userId}/district/{district}")
    public ResponseEntity<Map<String, Object>> getAddressesByUserAndDistrict(
            @PathVariable Long userId,
            @PathVariable String district) {
        
        List<Address> addresses = addressService.getAddressesByUserIdAndDistrict(userId, district);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Addresses retrieved successfully");
        response.put("count", addresses.size());
        response.put("data", addresses);
        return ResponseEntity.ok(response);
    }
    
    // GET count of user's addresses
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Map<String, Object>> countUserAddresses(@PathVariable Long userId) {
        Long count = addressService.countUserAddresses(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Address count retrieved successfully");
        response.put("count", count);
        return ResponseEntity.ok(response);
    }
}