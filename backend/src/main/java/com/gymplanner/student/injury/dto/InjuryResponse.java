package com.gymplanner.student.injury.dto;

import com.gymplanner.student.injury.InjurySeverity;
import java.time.Instant;
import java.time.LocalDate;

public record InjuryResponse(
        Long id,
        String bodyArea,
        String description,
        InjurySeverity severity,
        LocalDate startedAt,
        LocalDate resolvedAt,
        boolean active,
        String notes,
        Instant createdAt,
        Instant updatedAt) {
}
