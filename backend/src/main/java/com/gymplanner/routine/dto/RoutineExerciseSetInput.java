package com.gymplanner.routine.dto;

import com.gymplanner.shared.blocks.SetKind;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RoutineExerciseSetInput(Integer setNumber, SetKind setKind, Integer targetReps, Integer targetRepsMin, Integer targetRepsMax, BigDecimal targetWeightKg, Integer targetTimeSeconds, BigDecimal targetDistanceMeters, Integer restAfterSeconds, @Size(max = 20) String tempo, Integer rpe, String notes, Boolean toFailure) {}
