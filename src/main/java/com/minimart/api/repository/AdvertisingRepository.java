package com.minimart.api.repository;

import com.minimart.api.model.Advertising;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AdvertisingRepository extends JpaRepository<Advertising, Integer> {
    List<Advertising> findAllByOrderByCreateDateDesc();
    List<Advertising> findByIsActiveTrueOrderByCreateDateDesc();

}
