package com.gymplanner.exercise.dto;

import com.gymplanner.exercise.MeasurementType;
import com.gymplanner.exercise.tag.dto.TagResponse;
import java.time.Instant;
import java.util.List;

public record ExerciseResponse(
        Long id,
        String name,
        String slug,
        String description,
        String technicalNotes,
        MeasurementType defaultMeasurement,
        String videoUrl,
        String imageUrl,
        boolean active,
        List<TagResponse> tags,
        Instant createdAt,
        Instant updatedAt) {
}
