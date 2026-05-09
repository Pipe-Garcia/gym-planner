package com.gymplanner.student.injury.dto;

import com.gymplanner.student.injury.InjurySeverity;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateInjuryRequest(
        @Size(max = 100) String bodyArea,
        String description,
        InjurySeverity severity,
        @PastOrPresent LocalDate startedAt,
        LocalDate resolvedAt,
        Boolean active,
        String notes) {
}
