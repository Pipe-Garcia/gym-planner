package com.gymplanner.routine.dto;

import com.gymplanner.routine.RoutineStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RoutineResponse(Long id, Long studentId, String studentName, String name, String objective, Long sourceTemplateId, String sourceTemplateName, RoutineStatus status, LocalDate assignedDate, LocalDate finishedDate, Instant finishedAt, String generalNotes, String internalNotes, String closureNotes, Long previousRoutineId, Long createdByUserId, List<RoutineDayResponse> days, Instant createdAt, Instant updatedAt) {}
