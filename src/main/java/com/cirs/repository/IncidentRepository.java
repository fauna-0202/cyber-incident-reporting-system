package com.cirs.repository;

import com.cirs.entity.Incident;
import com.cirs.entity.IncidentCategory;
import com.cirs.entity.User;
import com.cirs.enums.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {
    Optional<Incident> findByIncidentNumber(String incidentNumber);
    boolean existsByIncidentNumber(String incidentNumber);

    List<Incident> findByReporterOrderByCreatedAtDesc(User reporter);
    Page<Incident> findByReporterOrderByCreatedAtDesc(User reporter, Pageable pageable);

    List<Incident> findByAssignedInvestigatorOrderByCreatedAtDesc(User investigator);
    Page<Incident> findByAssignedInvestigatorOrderByCreatedAtDesc(User investigator, Pageable pageable);

    long countByStatus(IncidentStatus status);
    long countByCategory(IncidentCategory category);

    @Query("SELECT i FROM Incident i WHERE " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:categoryId IS NULL OR i.category.id = :categoryId) AND " +
           "(:query IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(i.incidentNumber) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Incident> searchIncidents(@Param("status") IncidentStatus status,
                                  @Param("categoryId") Long categoryId,
                                  @Param("query") String query,
                                  Pageable pageable);
}
