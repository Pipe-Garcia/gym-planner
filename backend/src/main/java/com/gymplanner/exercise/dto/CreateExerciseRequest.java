package com.gymplanner.exercise.dto;

import com.gymplanner.exercise.MeasurementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateExerciseRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        String technicalNotes,
        MeasurementType defaultMeasurement,
        @Size(max = 500) String videoUrl,
        @Size(max = 500) String imageUrl,
        List<Long> tagIds) {
}
