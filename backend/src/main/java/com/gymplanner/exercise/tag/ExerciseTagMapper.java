package com.gymplanner.exercise.tag;

import com.gymplanner.exercise.tag.dto.TagResponse;
import org.springframework.stereotype.Component;

@Component
public class ExerciseTagMapper {

    public TagResponse toResponse(ExerciseTag tag) {
        return new TagResponse(tag.getId(), tag.getType(), tag.getName(), tag.getSlug());
    }
}
