package com.gymplanner.student.history.dto;

import com.gymplanner.routine.RoutineStatus;
import java.time.LocalDate;

public record StudentRoutineTimelineItemResponse(
        Long routineId,
        String routineName,
        RoutineStatus status,
        LocalDate assignedDate,
        LocalDate finishedDate,
        Long durationDays,
        long daysCount,
        long blocksCount,
        long exercisesCount,
        Long sourceTemplateId,
        String sourceTemplateName,
        String closureNotes) {
}
