package com.gymplanner.student.history.dto;

import com.gymplanner.shared.blocks.SetKind;
import java.math.BigDecimal;

public record StudentExerciseOccurrenceSetResponse(
        int setNumber,
        SetKind setKind,
        Integer targetReps,
        Integer targetRepsMin,
        Integer targetRepsMax,
        BigDecimal targetWeightKg,
        Integer targetTimeSeconds,
        BigDecimal targetDistanceMeters,
        Integer restAfterSeconds,
        String tempo,
        Integer rpe,
        boolean toFailure,
        String notes,
        String executionCue) {
}
