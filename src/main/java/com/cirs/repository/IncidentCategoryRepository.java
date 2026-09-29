package com.cirs.repository;

import com.cirs.entity.IncidentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentCategoryRepository extends JpaRepository<IncidentCategory, Long> {
    Optional<IncidentCategory> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<IncidentCategory> findByIsActiveTrueOrderByNameAsc();
    List<IncidentCategory> findAllByOrderByNameAsc();
}
