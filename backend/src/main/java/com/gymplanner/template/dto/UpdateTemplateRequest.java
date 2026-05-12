package com.gymplanner.template.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateTemplateRequest(@NotBlank @Size(max = 150) String name, String description, @Size(max = 100) String sport, @Size(max = 150) String objective, @Size(max = 50) String level, Integer estimatedDurationMinutes, String generalNotes, Boolean active, @Valid List<TemplateDayInput> days) {}
