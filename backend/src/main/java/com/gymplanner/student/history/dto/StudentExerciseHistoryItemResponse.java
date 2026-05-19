package com.gymplanner.student.history.dto;

import com.gymplanner.shared.blocks.BlockStructuralType;
import java.time.LocalDate;
import java.util.List;

public record StudentExerciseHistoryItemResponse(
        Long exerciseId,
        String exerciseName,
        LocalDate lastPerformedDate,
        Long lastRoutineId,
        String lastRoutineName,
        long timesUsed,
        List<BlockStructuralType> structuralTypesUsed,
        BlockStructuralType lastStructuralType) {
}
