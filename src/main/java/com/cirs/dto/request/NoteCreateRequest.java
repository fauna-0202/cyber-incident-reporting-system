package com.cirs.dto.request;

import com.cirs.enums.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class NoteCreateRequest {

    @NotNull(message = "Note type is required")
    private NoteType noteType;

    @NotBlank(message = "Note content cannot be empty")
    private String content;

    public NoteCreateRequest() {}

    public NoteCreateRequest(NoteType noteType, String content) {
        this.noteType = noteType;
        this.content = content;
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
}
