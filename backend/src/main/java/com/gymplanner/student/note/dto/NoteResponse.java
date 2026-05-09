package com.gymplanner.student.note.dto;

import java.time.Instant;

public record NoteResponse(
        Long id,
        String content,
        Long authorUserId,
        String authorName,
        Instant createdAt,
        Instant updatedAt) {
}
