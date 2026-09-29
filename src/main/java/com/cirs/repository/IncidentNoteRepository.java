package com.cirs.repository;

import com.cirs.entity.Incident;
import com.cirs.entity.IncidentNote;
import com.cirs.enums.NoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentNoteRepository extends JpaRepository<IncidentNote, Long> {
    List<IncidentNote> findByIncidentOrderByCreatedAtAsc(Incident incident);
    List<IncidentNote> findByIncidentAndNoteTypeNotOrderByCreatedAtAsc(Incident incident, NoteType excludeType);
}
