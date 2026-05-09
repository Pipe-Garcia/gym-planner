package com.gymplanner.student.dto;

import java.time.Instant;
import java.time.LocalDate;

public record StudentSummaryResponse(
        Long id,
        String firstName,
        String lastName,
        String documentId,
        String phone,
        String email,
        String sport,
        String level,
        boolean active,
        LocalDate startedAt,
        Instant createdAt,
        Instant updatedAt) {
}
