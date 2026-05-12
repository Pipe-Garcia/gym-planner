package com.gymplanner.template.dto;

import java.time.Instant;

public record TemplateSummaryResponse(Long id, String name, String description, String sport, String objective, String level, Integer estimatedDurationMinutes, boolean active, long dayCount, long blockCount, long exerciseCount, Instant createdAt, Instant updatedAt) {}
