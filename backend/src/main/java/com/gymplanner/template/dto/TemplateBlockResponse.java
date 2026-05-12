package com.gymplanner.template.dto;

import com.gymplanner.shared.blocks.BlockPurpose;
import com.gymplanner.shared.blocks.BlockStructuralType;
import java.util.List;

public record TemplateBlockResponse(Long id, int orderIndex, String title, BlockStructuralType structuralType, BlockPurpose purpose, Integer totalDurationSeconds, Integer targetRounds, String blockNotes, List<TemplateExerciseResponse> exercises) {}
