package com.gymplanner.exercise;

import com.gymplanner.exercise.dto.ExerciseResponse;
import com.gymplanner.exercise.dto.ExerciseSummaryResponse;
import com.gymplanner.exercise.tag.ExerciseTagMapper;
import java.util.Comparator;
import org.springframework.stereotype.Component;

@Component
public class ExerciseMapper {

    private final ExerciseTagMapper tagMapper;

    public ExerciseMapper(ExerciseTagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    public ExerciseSummaryResponse toSummary(Exercise exercise) {
        return new ExerciseSummaryResponse(
                exercise.getId(),
                exercise.getName(),
                exercise.getSlug(),
                exercise.getDefaultMeasurement(),
                exercise.isActive(),
                tags(exercise),
                exercise.getCreatedAt(),
                exercise.getUpdatedAt());
    }

    public ExerciseResponse toResponse(Exercise exercise) {
        return new ExerciseResponse(
                exercise.getId(),
                exercise.getName(),
                exercise.getSlug(),
                exercise.getDescription(),
                exercise.getTechnicalNotes(),
                exercise.getDefaultMeasurement(),
                exercise.getVideoUrl(),
                exercise.getImageUrl(),
                exercise.isActive(),
                tags(exercise),
                exercise.getCreatedAt(),
                exercise.getUpdatedAt());
    }

    private java.util.List<com.gymplanner.exercise.tag.dto.TagResponse> tags(Exercise exercise) {
        return exercise.getTags().stream()
                .sorted(Comparator.comparing(tag -> tag.getType().name() + tag.getName()))
                .map(tagMapper::toResponse)
                .toList();
    }
}
