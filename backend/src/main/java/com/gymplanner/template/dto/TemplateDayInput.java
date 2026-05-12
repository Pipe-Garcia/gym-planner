package com.gymplanner.template.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TemplateDayInput(Long id, Integer orderIndex, @NotBlank @Size(max = 150) String name, String notes, @Valid List<TemplateBlockInput> blocks) {}
