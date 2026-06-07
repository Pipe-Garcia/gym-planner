package com.gymplanner.routine.dto;

import com.gymplanner.shared.blocks.BlockPurpose;
import com.gymplanner.shared.blocks.BlockStructuralType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RoutineBlockInput(Integer orderIndex, @NotBlank @Size(max = 150) String title, @NotNull BlockStructuralType structuralType, BlockPurpose purpose, Integer totalDurationSeconds, Integer targetRounds, @Min(0) Integer roundRestSeconds, String blockNotes, @Valid List<RoutineExerciseInput> exercises) {
    public RoutineBlockInput(Integer orderIndex, String title, BlockStructuralType structuralType, BlockPurpose purpose, Integer totalDurationSeconds, Integer targetRounds, String blockNotes, List<RoutineExerciseInput> exercises) {
        this(orderIndex, title, structuralType, purpose, totalDurationSeconds, targetRounds, null, blockNotes, exercises);
    }
}
