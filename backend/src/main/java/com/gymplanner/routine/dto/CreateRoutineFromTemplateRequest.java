package com.gymplanner.routine.dto;

import com.gymplanner.routine.RoutineStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateRoutineFromTemplateRequest(@NotNull Long studentId, @NotNull Long templateId, @Size(max = 150) String name, LocalDate assignedDate, String generalNotes, String internalNotes, RoutineStatus status) {}
