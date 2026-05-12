package com.gymplanner.routine.dto;

import com.gymplanner.shared.blocks.BlockPurpose;
import com.gymplanner.shared.blocks.BlockStructuralType;
import java.util.List;

public record RoutineBlockResponse(Long id, int orderIndex, String title, BlockStructuralType structuralType, BlockPurpose purpose, Integer totalDurationSeconds, Integer targetRounds, String blockNotes, List<RoutineExerciseResponse> exercises) {}
