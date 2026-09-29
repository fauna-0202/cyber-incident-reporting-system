package com.cirs.dto.response;

import com.cirs.enums.NoteType;
import com.cirs.enums.Role;
import java.time.LocalDateTime;

public class IncidentNoteResponse {
    private Long id;
    private String authorName;
    private Role authorRole;
    private NoteType noteType;
    private String content;
    private LocalDateTime createdAt;

    public IncidentNoteResponse() {}

    public IncidentNoteResponse(Long id, String authorName, Role authorRole, NoteType noteType,
                                String content, LocalDateTime createdAt) {
        this.id = id;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.noteType = noteType;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Role getAuthorRole() {
        return authorRole;
    }

    public void setAuthorRole(Role authorRole) {
        this.authorRole = authorRole;
    }

    public NoteType getNoteType() {
        return noteType;
    }

    public void setNoteType(NoteType noteType) {
        this.noteType = noteType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
