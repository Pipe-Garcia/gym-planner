package com.gymplanner.exercise.tag.dto;

import com.gymplanner.exercise.tag.TagType;

public record TagResponse(Long id, TagType type, String name, String slug) {
}
