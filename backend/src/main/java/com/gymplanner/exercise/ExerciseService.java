package com.gymplanner.exercise;

import com.gymplanner.exercise.dto.CreateExerciseRequest;
import com.gymplanner.exercise.dto.ExerciseResponse;
import com.gymplanner.exercise.dto.ExerciseSummaryResponse;
import com.gymplanner.exercise.dto.UpdateExerciseRequest;
import com.gymplanner.exercise.tag.ExerciseTag;
import com.gymplanner.exercise.tag.ExerciseTagRepository;
import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymRepository;
import com.gymplanner.shared.exception.BusinessRuleException;
import com.gymplanner.shared.exception.ConflictException;
import com.gymplanner.shared.exception.NotFoundException;
import com.gymplanner.shared.pagination.PageResponse;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseTagRepository tagRepository;
    private final GymRepository gymRepository;
    private final ExerciseMapper exerciseMapper;

    @Transactional(readOnly = true)
    public PageResponse<ExerciseSummaryResponse> list(
            Long gymId,
            String search,
            List<Long> tagIds,
            Boolean active,
            Pageable pageable) {
        Page<Exercise> page = exerciseRepository.findAll(specification(gymId, search, tagIds, active), pageable);
        return new PageResponse<>(
                page.map(exerciseMapper::toSummary).getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional
    public ExerciseResponse create(Long gymId, CreateExerciseRequest request) {
        String name = request.name().trim();
        String slug = SlugUtils.toSlug(name);
        validateSlugAvailable(gymId, slug, null);

        Exercise exercise = new Exercise();
        exercise.setGym(getGym(gymId));
        exercise.setName(name);
        exercise.setSlug(slug);
        exercise.setDescription(clean(request.description()));
        exercise.setTechnicalNotes(clean(request.technicalNotes()));
        exercise.setDefaultMeasurement(request.defaultMeasurement() == null
                ? MeasurementType.REPS_WEIGHT
                : request.defaultMeasurement());
        exercise.setVideoUrl(clean(request.videoUrl()));
        exercise.setImageUrl(clean(request.imageUrl()));
        exercise.setTags(resolveTags(gymId, request.tagIds()));
        return exerciseMapper.toResponse(exerciseRepository.save(exercise));
    }

    @Transactional(readOnly = true)
    public ExerciseResponse get(Long gymId, Long id) {
        return exerciseMapper.toResponse(getEntity(gymId, id));
    }

    @Transactional
    public ExerciseResponse update(Long gymId, Long id, UpdateExerciseRequest request) {
        Exercise exercise = getEntity(gymId, id);
        if (StringUtils.hasText(request.name())) {
            String name = request.name().trim();
            String slug = SlugUtils.toSlug(name);
            validateSlugAvailable(gymId, slug, id);
            exercise.setName(name);
            exercise.setSlug(slug);
        }
        if (request.description() != null) {
            exercise.setDescription(clean(request.description()));
        }
        if (request.technicalNotes() != null) {
            exercise.setTechnicalNotes(clean(request.technicalNotes()));
        }
        if (request.defaultMeasurement() != null) {
            exercise.setDefaultMeasurement(request.defaultMeasurement());
        }
        if (request.videoUrl() != null) {
            exercise.setVideoUrl(clean(request.videoUrl()));
        }
        if (request.imageUrl() != null) {
            exercise.setImageUrl(clean(request.imageUrl()));
        }
        if (request.tagIds() != null) {
            exercise.setTags(resolveTags(gymId, request.tagIds()));
        }
        return exerciseMapper.toResponse(exerciseRepository.save(exercise));
    }

    @Transactional
    public void deactivate(Long gymId, Long id) {
        Exercise exercise = getEntity(gymId, id);
        exercise.setActive(false);
        exerciseRepository.save(exercise);
    }

    @Transactional
    public ExerciseResponse reactivate(Long gymId, Long id) {
        Exercise exercise = getEntity(gymId, id);
        exercise.setActive(true);
        return exerciseMapper.toResponse(exerciseRepository.save(exercise));
    }

    @Transactional(readOnly = true)
    public Exercise getEntity(Long gymId, Long id) {
        return exerciseRepository.findByIdAndGymId(id, gymId)
                .orElseThrow(() -> new NotFoundException("Exercise not found."));
    }

    private Set<ExerciseTag> resolveTags(Long gymId, List<Long> tagIds) {
        if (CollectionUtils.isEmpty(tagIds)) {
            return new LinkedHashSet<>();
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(tagIds);
        List<ExerciseTag> tags = tagRepository.findByGymIdAndIdIn(gymId, uniqueIds);
        if (tags.size() != uniqueIds.size()) {
            throw new BusinessRuleException("All tags must exist and belong to the current gym.");
        }
        return new LinkedHashSet<>(tags);
    }

    private void validateSlugAvailable(Long gymId, String slug, Long currentExerciseId) {
        boolean exists = currentExerciseId == null
                ? exerciseRepository.existsByGymIdAndSlug(gymId, slug)
                : exerciseRepository.existsByGymIdAndSlugAndIdNot(gymId, slug, currentExerciseId);
        if (exists) {
            throw new ConflictException("An exercise with this name already exists in this gym.");
        }
    }

    private Gym getGym(Long gymId) {
        return gymRepository.findById(gymId)
                .orElseThrow(() -> new NotFoundException("Gym not found."));
    }

    private Specification<Exercise> specification(Long gymId, String search, List<Long> tagIds, Boolean active) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("gym").get("id"), gymId));
            if (active != null) {
                predicates.add(builder.equal(root.get("active"), active));
            }
            if (StringUtils.hasText(search)) {
                predicates.add(builder.like(builder.lower(root.get("name")), "%" + search.trim().toLowerCase() + "%"));
            }
            if (!CollectionUtils.isEmpty(tagIds)) {
                for (Long tagId : new LinkedHashSet<>(tagIds)) {
                    var subquery = query.subquery(Long.class);
                    var subRoot = subquery.from(Exercise.class);
                    var tagJoin = subRoot.join("tags", JoinType.INNER);
                    subquery.select(subRoot.get("id"))
                            .where(
                                    builder.equal(subRoot.get("id"), root.get("id")),
                                    builder.equal(tagJoin.get("id"), tagId));
                    predicates.add(builder.exists(subquery));
                }
            }
            query.distinct(true);
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
