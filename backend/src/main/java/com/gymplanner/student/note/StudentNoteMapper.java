package com.gymplanner.student.note;

import com.gymplanner.student.note.dto.NoteResponse;
import org.springframework.stereotype.Component;

@Component
public class StudentNoteMapper {

    public NoteResponse toResponse(StudentNote note) {
        return new NoteResponse(
                note.getId(),
                note.getContent(),
                note.getAuthorUser().getId(),
                note.getAuthorUser().getFullName(),
                note.getCreatedAt(),
                note.getUpdatedAt());
    }
}
