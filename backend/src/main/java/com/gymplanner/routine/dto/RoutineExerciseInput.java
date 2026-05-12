package com.gymplanner.routine.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RoutineExerciseInput(@NotNull Long exerciseId, Integer orderIndex, String exerciseNotes, @Valid List<RoutineExerciseSetInput> sets) {}
