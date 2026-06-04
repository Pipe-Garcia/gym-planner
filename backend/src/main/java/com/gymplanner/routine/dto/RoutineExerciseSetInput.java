package com.gymplanner.routine.dto;

import com.gymplanner.shared.blocks.SetKind;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RoutineExerciseSetInput(
        Integer setNumber,
        SetKind setKind,
        Integer targetReps,
        Integer targetRepsMin,
        Integer targetRepsMax,
        BigDecimal targetWeightKg,
        Integer targetTimeSeconds,
        BigDecimal targetDistanceMeters,
        Integer restAfterSeconds,
        @Size(max = 20) String tempo,
        @Size(max = 120) String executionCue,
        Integer rpe,
        String notes,
        Boolean toFailure) {
    public RoutineExerciseSetInput(
            Integer setNumber,
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
            String notes,
            Boolean toFailure) {
        this(setNumber, setKind, targetReps, targetRepsMin, targetRepsMax, targetWeightKg, targetTimeSeconds, targetDistanceMeters, restAfterSeconds, tempo, null, rpe, notes, toFailure);
    }
}
