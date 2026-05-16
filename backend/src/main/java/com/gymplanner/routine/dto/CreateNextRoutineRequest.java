package com.gymplanner.routine.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateNextRoutineRequest(
        @Size(max = 150) String newRoutineName,
        @NotNull LocalDate newAssignedDate,
        @Pattern(regexp = "DRAFT|ACTIVE", message = "newStatus debe ser DRAFT o ACTIVE")
        String newStatus,
        Boolean copyGeneralNotes,
        Boolean copyInternalNotes,
        @Valid WeightAdjustmentInput weightAdjustment
) {}
