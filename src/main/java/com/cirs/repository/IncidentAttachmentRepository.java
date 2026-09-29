package com.cirs.repository;

import com.cirs.entity.Incident;
import com.cirs.entity.IncidentAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentAttachmentRepository extends JpaRepository<IncidentAttachment, Long> {
    List<IncidentAttachment> findByIncidentOrderByUploadedAtAsc(Incident incident);
    Optional<IncidentAttachment> findByStoredFilename(String storedFilename);
    Optional<IncidentAttachment> findByIdAndIncident(Long id, Incident incident);
}
