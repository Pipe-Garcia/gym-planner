package com.gymplanner.student.history.dto;

import java.time.LocalDate;

public record StudentHistorySummaryResponse(
        Long studentId,
        String studentFullName,
        long totalRoutines,
        Long activeRoutineId,
        String activeRoutineName,
        LocalDate activeRoutineAssignedDate,
        Long activeRoutineDaysCount,
        long distinctExercisesCount,
        LocalDate trainingSince) {
}
