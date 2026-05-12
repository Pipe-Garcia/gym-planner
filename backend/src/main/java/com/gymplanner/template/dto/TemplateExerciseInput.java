package com.gymplanner.template.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TemplateExerciseInput(@NotNull Long exerciseId, Integer orderIndex, String exerciseNotes, @Valid List<TemplateExerciseSetInput> sets) {}
