package com.gymplanner.exercise.tag.dto;

import com.gymplanner.exercise.tag.TagType;

public record TagUsageResponse(Long id, TagType type, String name, String slug, long usageCount) {
}
