package com.minimart.api.service;

import com.minimart.api.model.Address;
import com.minimart.api.repository.AddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AddressService {
    
    @Autowired
    private AddressRepository addressRepository;
    
    // Get ALL addresses (for admin/owner)
    public List<Address> getAllAddresses() {
        return addressRepository.findAll();
    }
    
    // Get all addresses for a specific user
    public List<Address> getAllAddressesByUserId(Long userId) {
        return addressRepository.findByUserId(userId);
    }
    
    // Get address by ID (for anyone)
    public Optional<Address> getAddressById(Integer id) {
        return addressRepository.findById(id);
    }
    
    // Create new address
    public Address createAddress(Address address) {
        return addressRepository.save(address);
    }
    
    // Update address by ID only (user_id CANNOT be changed)
    public Address updateAddressById(Integer id, Address addressDetails) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + id));
        
        // Update all fields EXCEPT user_id (user_id should never change!)
        address.setName(addressDetails.getName());
        address.setHome_no(addressDetails.getHome_no());
        address.setStreet(addressDetails.getStreet());
        address.setDistrict(addressDetails.getDistrict());
        address.setProvince(addressDetails.getProvince());
        address.setLatitude(addressDetails.getLatitude());
        address.setLongitude(addressDetails.getLongitude());
        // DO NOT UPDATE: address.setUser_id() - user_id stays the same!
        
        return addressRepository.save(address);
    }
    
    // Delete address by ID (no user check)
    public void deleteAddressById(Integer id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + id));
        addressRepository.delete(address);
    }
    
    // Find addresses by user and province
    public List<Address> getAddressesByUserIdAndProvince(Long userId, String province) {
        return addressRepository.findByUserIdAndProvince(userId, province);
    }
    
    // Find addresses by user and district
    public List<Address> getAddressesByUserIdAndDistrict(Long userId, String district) {
        return addressRepository.findByUserIdAndDistrict(userId, district);
    }
    
    // Count user's addresses
    public Long countUserAddresses(Long userId) {
        return addressRepository.countByUserId(userId);
    }
}
