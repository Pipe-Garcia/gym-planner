package com.gymplanner.student.note.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateNoteRequest(@NotBlank String content) {
}
