package com.gymplanner.template.dto;

import com.gymplanner.exercise.MeasurementType;
import java.util.List;

public record TemplateExerciseResponse(Long id, int orderIndex, Long exerciseId, String exerciseName, boolean exerciseActive, MeasurementType defaultMeasurement, String exerciseNotes, List<TemplateExerciseSetResponse> sets) {}
