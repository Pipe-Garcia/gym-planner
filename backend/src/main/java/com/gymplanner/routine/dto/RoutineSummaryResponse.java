package com.gymplanner.routine.dto;

import com.gymplanner.routine.RoutineStatus;
import java.time.Instant;
import java.time.LocalDate;

public record RoutineSummaryResponse(Long id, Long studentId, String studentName, String name, String objective, Long sourceTemplateId, String sourceTemplateName, RoutineStatus status, LocalDate assignedDate, LocalDate finishedDate, long dayCount, long blockCount, long exerciseCount, Instant createdAt, Instant updatedAt) {}
