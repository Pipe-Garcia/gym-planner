package com.gymplanner.exercise.tag;

import com.gymplanner.exercise.tag.dto.CreateTagRequest;
import com.gymplanner.exercise.tag.dto.TagResponse;
import com.gymplanner.exercise.tag.dto.TagUsageResponse;
import com.gymplanner.exercise.tag.dto.UpdateTagRequest;
import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymRepository;
import com.gymplanner.shared.exception.ConflictException;
import com.gymplanner.shared.exception.NotFoundException;
import com.gymplanner.shared.util.SlugUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExerciseTagService {

    private final ExerciseTagRepository tagRepository;
    private final ExerciseTagMapper tagMapper;
    private final GymRepository gymRepository;

    @Transactional(readOnly = true)
    public List<TagResponse> list(Long gymId, TagType type) {
        List<ExerciseTag> tags = type == null
                ? tagRepository.findByGymIdOrderByTypeAscNameAsc(gymId)
                : tagRepository.findByGymIdAndTypeOrderByNameAsc(gymId, type);
        return tags.stream().map(tagMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TagUsageResponse> listUsage(Long gymId, TagType type) {
        return tagRepository.findUsageByGymIdAndOptionalType(gymId, type);
    }

    @Transactional
    public TagResponse create(Long gymId, CreateTagRequest request) {
        String name = request.name().trim();
        String slug = SlugUtils.toSlug(name);
        validateSlugAvailable(gymId, request.type(), slug, null);

        ExerciseTag tag = new ExerciseTag();
        tag.setGym(getGym(gymId));
        tag.setType(request.type());
        tag.setName(name);
        tag.setSlug(slug);
        return save(tag);
    }

    @Transactional
    public TagResponse update(Long gymId, Long id, UpdateTagRequest request) {
        ExerciseTag tag = getEntity(gymId, id);
        String name = request.name().trim();
        String slug = SlugUtils.toSlug(name);
        validateSlugAvailable(gymId, tag.getType(), slug, id);

        tag.setName(name);
        tag.setSlug(slug);
        return save(tag);
    }

    @Transactional
    public void delete(Long gymId, Long id) {
        ExerciseTag tag = getEntity(gymId, id);
        tagRepository.deleteOwnedTag(tag.getId(), gymId);
    }

    private ExerciseTag getEntity(Long gymId, Long id) {
        return tagRepository.findByIdAndGymId(id, gymId)
                .orElseThrow(() -> new NotFoundException("Tag de ejercicio no encontrado."));
    }

    private Gym getGym(Long gymId) {
        return gymRepository.findById(gymId)
                .orElseThrow(() -> new NotFoundException("Gym not found."));
    }

    private void validateSlugAvailable(Long gymId, TagType type, String slug, Long currentTagId) {
        boolean exists = currentTagId == null
                ? tagRepository.existsByGymIdAndTypeAndSlug(gymId, type, slug)
                : tagRepository.existsByGymIdAndTypeAndSlugAndIdNot(gymId, type, slug, currentTagId);
        if (exists) {
            throw duplicateConflict();
        }
    }

    private TagResponse save(ExerciseTag tag) {
        try {
            return tagMapper.toResponse(tagRepository.saveAndFlush(tag));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateConflict();
        }
    }

    private ConflictException duplicateConflict() {
        return new ConflictException("Ya existe un tag de ese tipo con ese nombre.");
    }
}
