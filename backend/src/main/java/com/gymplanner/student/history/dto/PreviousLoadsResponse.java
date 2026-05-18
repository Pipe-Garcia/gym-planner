package com.gymplanner.student.history.dto;

import java.util.List;

public record PreviousLoadsResponse(
        Long exerciseId,
        String exerciseName,
        boolean found,
        List<PreviousLoadOccurrence> occurrences) {
}
