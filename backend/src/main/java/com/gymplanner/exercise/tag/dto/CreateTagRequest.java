package com.gymplanner.exercise.tag.dto;

import com.gymplanner.exercise.tag.TagType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTagRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull TagType type) {
}
