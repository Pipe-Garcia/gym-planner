package com.gymplanner.routine.dto;

import com.gymplanner.shared.blocks.SetKind;
import java.math.BigDecimal;

public record RoutineExerciseSetResponse(Long id, int setNumber, SetKind setKind, Integer targetReps, Integer targetRepsMin, Integer targetRepsMax, BigDecimal targetWeightKg, Integer targetTimeSeconds, BigDecimal targetDistanceMeters, Integer restAfterSeconds, String tempo, Integer rpe, String notes, boolean toFailure) {}
