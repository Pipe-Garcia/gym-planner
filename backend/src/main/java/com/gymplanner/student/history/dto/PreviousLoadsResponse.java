package com.gymplanner.student.history.dto;

import com.gymplanner.shared.blocks.BlockStructuralType;
import com.gymplanner.student.history.PreviousLoadsMatchType;
import java.util.List;

public record PreviousLoadsResponse(
        Long exerciseId,
        String exerciseName,
        boolean found,
        PreviousLoadsMatchType matchType,
        BlockStructuralType requestedStructuralType,
        List<PreviousLoadOccurrence> occurrences) {
}
