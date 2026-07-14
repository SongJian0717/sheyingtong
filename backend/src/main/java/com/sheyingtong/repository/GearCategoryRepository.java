package com.sheyingtong.repository;

import com.sheyingtong.entity.GearCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GearCategoryRepository extends JpaRepository<GearCategory, Long> {
    
    Optional<GearCategory> findByName(String name);
}
