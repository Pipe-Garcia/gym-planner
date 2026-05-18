package com.gymplanner.student.history.dto;

import com.gymplanner.shared.blocks.SetKind;
import java.math.BigDecimal;

public record PreviousLoadSet(
        int setNumber,
        SetKind setKind,
        Integer targetReps,
        Integer targetRepsMin,
        Integer targetRepsMax,
        BigDecimal targetWeightKg,
        Integer targetTimeSeconds,
        BigDecimal targetDistanceMeters,
        Integer restAfterSeconds,
        Integer rpe,
        boolean toFailure) {
}
