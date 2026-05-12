package com.gymplanner.routine.dto;

import com.gymplanner.exercise.MeasurementType;
import com.gymplanner.exercise.dto.ExerciseResponse;
import java.util.List;

public record RoutineExerciseResponse(Long id, int orderIndex, Long exerciseId, String exerciseName, boolean exerciseActive, MeasurementType defaultMeasurement, ExerciseResponse exercise, String exerciseNotes, List<RoutineExerciseSetResponse> sets) {}
