package com.gymplanner.student.injury.dto;

import com.gymplanner.student.injury.InjurySeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateInjuryRequest(
        @NotBlank @Size(max = 100) String bodyArea,
        @NotBlank String description,
        @NotNull InjurySeverity severity,
        @PastOrPresent LocalDate startedAt,
        String notes) {
}
