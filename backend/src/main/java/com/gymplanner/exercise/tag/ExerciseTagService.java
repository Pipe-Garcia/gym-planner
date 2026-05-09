package com.gymplanner.exercise.tag;

import com.gymplanner.exercise.tag.dto.TagResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExerciseTagService {

    private final ExerciseTagRepository tagRepository;
    private final ExerciseTagMapper tagMapper;

    @Transactional(readOnly = true)
    public List<TagResponse> list(Long gymId, TagType type) {
        List<ExerciseTag> tags = type == null
                ? tagRepository.findByGymIdOrderByTypeAscNameAsc(gymId)
                : tagRepository.findByGymIdAndTypeOrderByNameAsc(gymId, type);
        return tags.stream().map(tagMapper::toResponse).toList();
    }
}
