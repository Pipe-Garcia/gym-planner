package com.gymplanner.student.dto;

import com.gymplanner.student.injury.dto.InjuryResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record StudentResponse(
        Long id,
        String firstName,
        String lastName,
        String documentId,
        String phone,
        String email,
        LocalDate birthDate,
        String sport,
        String objective,
        String level,
        String generalNotes,
        boolean active,
        LocalDate startedAt,
        List<InjuryResponse> activeInjuries,
        Instant createdAt,
        Instant updatedAt) {
}
