package com.gymplanner.exercise.dto;

import com.gymplanner.exercise.MeasurementType;
import com.gymplanner.exercise.tag.dto.TagResponse;
import java.time.Instant;
import java.util.List;

public record ExerciseSummaryResponse(
        Long id,
        String name,
        String slug,
        MeasurementType defaultMeasurement,
        boolean active,
        List<TagResponse> tags,
        Instant createdAt,
        Instant updatedAt) {
}
