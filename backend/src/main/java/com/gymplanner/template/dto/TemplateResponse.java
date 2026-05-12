package com.gymplanner.template.dto;

import java.time.Instant;
import java.util.List;

public record TemplateResponse(Long id, String name, String description, String sport, String objective, String level, Integer estimatedDurationMinutes, String generalNotes, boolean active, Long createdByUserId, List<TemplateDayResponse> days, Instant createdAt, Instant updatedAt) {}
