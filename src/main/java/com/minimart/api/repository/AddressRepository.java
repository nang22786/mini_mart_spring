package com.minimart.api.repository;

import com.minimart.api.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Integer> {
    
    // Find all addresses by user_id
    @Query("SELECT a FROM Address a WHERE a.user_id = :user_id ORDER BY a.created_at DESC")
    List<Address> findByUserId(@Param("user_id") Long user_id);
    
    // Find address by id and user_id (for security - user can only access their own addresses)
    @Query("SELECT a FROM Address a WHERE a.id = :id AND a.user_id = :user_id")
    Optional<Address> findByIdAndUserId(@Param("id") Integer id, @Param("user_id") Long user_id);
    
    // Find addresses by user_id and province
    @Query("SELECT a FROM Address a WHERE a.user_id = :user_id AND a.province = :province")
    List<Address> findByUserIdAndProvince(@Param("user_id") Long user_id, @Param("province") String province);
    
    // Find addresses by user_id and district
    @Query("SELECT a FROM Address a WHERE a.user_id = :user_id AND a.district = :district")
    List<Address> findByUserIdAndDistrict(@Param("user_id") Long user_id, @Param("district") String district);
    
    // Find address by home_no and user_id
    @Query("SELECT a FROM Address a WHERE a.user_id = :user_id AND a.home_no = :home_no")
    List<Address> findByUserIdAndHomeNo(@Param("user_id") Long user_id, @Param("home_no") String home_no);
    
    // Count addresses by user_id
    @Query("SELECT COUNT(a) FROM Address a WHERE a.user_id = :user_id")
    Long countByUserId(@Param("user_id") Long user_id);
}