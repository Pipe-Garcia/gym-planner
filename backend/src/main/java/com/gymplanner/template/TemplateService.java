package com.gymplanner.template;

import com.gymplanner.exercise.ExerciseService;
import com.gymplanner.gym.GymRepository;
import com.gymplanner.shared.blocks.BlockStructuralType;
import com.gymplanner.shared.blocks.SetKind;
import com.gymplanner.shared.exception.NotFoundException;
import com.gymplanner.shared.pagination.PageResponse;
import com.gymplanner.template.dto.CreateTemplateRequest;
import com.gymplanner.template.dto.TemplateBlockInput;
import com.gymplanner.template.dto.TemplateDayInput;
import com.gymplanner.template.dto.TemplateExerciseInput;
import com.gymplanner.template.dto.TemplateExerciseSetInput;
import com.gymplanner.template.dto.TemplateResponse;
import com.gymplanner.template.dto.TemplateSummaryResponse;
import com.gymplanner.template.dto.UpdateTemplateRequest;
import com.gymplanner.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {
    private final TrainingTemplateRepository templateRepository;
    private final GymRepository gymRepository;
    private final UserRepository userRepository;
    private final ExerciseService exerciseService;
    private final TemplateValidator validator;
    private final TemplateMapper mapper;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public PageResponse<TemplateSummaryResponse> list(Long gymId, String search, String sport, String objective, String level, Boolean active, Pageable pageable) {
        Page<TrainingTemplate> page = templateRepository.findAll(specification(gymId, search, sport, objective, level, active), pageable);
        return new PageResponse<>(page.map(t -> mapper.toSummary(t, templateRepository.countDays(t.getId()), templateRepository.countBlocks(t.getId()), templateRepository.countExercises(t.getId()))).getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public TemplateResponse create(Long gymId, Long userId, CreateTemplateRequest request) {
        TrainingTemplate template = new TrainingTemplate();
        template.setGym(gymRepository.findById(gymId).orElseThrow(() -> new NotFoundException("Gym not found.")));
        template.setCreatedByUser(userRepository.getReferenceById(userId));
        applyMetadata(template, request.name(), request.description(), request.sport(), request.objective(), request.level(), request.estimatedDurationMinutes(), request.generalNotes(), true);
        template.getDays().addAll(mapDays(gymId, request.days(), template));
        validator.validate(template);
        return mapper.toResponse(templateRepository.save(template));
    }

    @Transactional(readOnly = true)
    public TemplateResponse get(Long gymId, Long id) {
        TrainingTemplate template = getFull(gymId, id);
        return mapper.toResponse(template);
    }

    @Transactional
    public TemplateResponse update(Long gymId, Long id, UpdateTemplateRequest request) {
        TrainingTemplate template = getFull(gymId, id);
        applyMetadata(template, request.name(), request.description(), request.sport(), request.objective(), request.level(), request.estimatedDurationMinutes(), request.generalNotes(), request.active() == null ? template.isActive() : request.active());
        template.getDays().clear();
        entityManager.flush();
        template.getDays().addAll(mapDays(gymId, request.days(), template));
        validator.validate(template);
        return mapper.toResponse(templateRepository.save(template));
    }

    @Transactional
    public void deactivate(Long gymId, Long id) {
        TrainingTemplate template = getEntity(gymId, id);
        template.setActive(false);
    }

    @Transactional
    public TemplateResponse reactivate(Long gymId, Long id) {
        TrainingTemplate template = getEntity(gymId, id);
        template.setActive(true);
        return mapper.toResponse(getFull(gymId, id));
    }

    @Transactional
    public TemplateResponse duplicate(Long gymId, Long userId, Long id) {
        TrainingTemplate original = getFull(gymId, id);
        TrainingTemplate copy = new TrainingTemplate();
        copy.setGym(original.getGym());
        copy.setCreatedByUser(userRepository.getReferenceById(userId));
        applyMetadata(copy, original.getName() + " (copia)", original.getDescription(), original.getSport(), original.getObjective(), original.getLevel(), original.getEstimatedDurationMinutes(), original.getGeneralNotes(), true);
        for (TemplateDay originalDay : original.getDays()) {
            copy.getDays().add(copyDay(originalDay, copy));
        }
        validator.validate(copy);
        return mapper.toResponse(templateRepository.save(copy));
    }

    @Transactional(readOnly = true)
    public TrainingTemplate getFull(Long gymId, Long id) {
        TrainingTemplate template = templateRepository.findByIdWithFullStructure(id)
                .orElseThrow(() -> new NotFoundException("Template not found."));
        if (!template.getGym().getId().equals(gymId)) {
            throw new NotFoundException("Template not found.");
        }
        return template;
    }

    @Transactional(readOnly = true)
    public TrainingTemplate getEntity(Long gymId, Long id) {
        return templateRepository.findByIdAndGymId(id, gymId).orElseThrow(() -> new NotFoundException("Template not found."));
    }

    private void applyMetadata(TrainingTemplate template, String name, String description, String sport, String objective, String level, Integer estimatedDurationMinutes, String generalNotes, boolean active) {
        template.setName(name.trim());
        template.setDescription(clean(description));
        template.setSport(clean(sport));
        template.setObjective(clean(objective));
        template.setLevel(clean(level));
        template.setEstimatedDurationMinutes(estimatedDurationMinutes);
        template.setGeneralNotes(clean(generalNotes));
        template.setActive(active);
    }

    private List<TemplateDay> mapDays(Long gymId, List<TemplateDayInput> inputs, TrainingTemplate template) {
        List<TemplateDay> days = new ArrayList<>();
        if (inputs == null) {
            return days;
        }
        int dayIndex = 1;
        for (TemplateDayInput input : inputs) {
            TemplateDay day = new TemplateDay();
            day.setTemplate(template);
            day.setOrderIndex(dayIndex++);
            day.setName(input.name().trim());
            day.setNotes(clean(input.notes()));
            day.getBlocks().addAll(mapBlocks(gymId, input.blocks(), day));
            days.add(day);
        }
        return days;
    }

    private List<TemplateBlock> mapBlocks(Long gymId, List<TemplateBlockInput> inputs, TemplateDay day) {
        List<TemplateBlock> blocks = new ArrayList<>();
        if (inputs == null) {
            return blocks;
        }
        int blockIndex = 1;
        for (TemplateBlockInput input : inputs) {
            TemplateBlock block = new TemplateBlock();
            block.setDay(day);
            block.setOrderIndex(blockIndex++);
            block.setTitle(input.title().trim());
            block.setStructuralType(input.structuralType());
            block.setPurpose(input.purpose());
            block.setTotalDurationSeconds(input.totalDurationSeconds());
            block.setTargetRounds(input.targetRounds());
            block.setBlockNotes(clean(input.blockNotes()));
            int exerciseIndex = 1;
            if (input.exercises() != null) {
                for (TemplateExerciseInput exerciseInput : input.exercises()) {
                    TemplateExercise exercise = new TemplateExercise();
                    exercise.setBlock(block);
                    exercise.setExercise(exerciseService.getEntity(gymId, exerciseInput.exerciseId()));
                    exercise.setOrderIndex(exerciseIndex++);
                    exercise.setExerciseNotes(clean(exerciseInput.exerciseNotes()));
                    addSets(exercise, exerciseInput.sets(), block.getStructuralType());
                    block.getExercises().add(exercise);
                }
            }
            blocks.add(block);
        }
        return blocks;
    }

    private void addSets(TemplateExercise exercise, List<TemplateExerciseSetInput> inputs, BlockStructuralType structuralType) {
        if (inputs == null) {
            return;
        }
        List<TemplateExerciseSetInput> effectiveInputs = inputs;
        if (structuralType == BlockStructuralType.CIRCUIT && inputs.size() > 1) {
            log.warn("Template circuit exercise {} received {} sets; keeping only the first.", exercise.getExercise().getId(), inputs.size());
            effectiveInputs = inputs.subList(0, 1);
        }
        int setNumber = 1;
        for (TemplateExerciseSetInput input : effectiveInputs) {
            TemplateExerciseSet set = new TemplateExerciseSet();
            set.setTemplateExercise(exercise);
            set.setSetNumber(setNumber++);
            set.setSetKind(input.setKind() == null ? SetKind.NORMAL : input.setKind());
            set.setTargetReps(input.targetReps());
            set.setTargetRepsMin(input.targetRepsMin());
            set.setTargetRepsMax(input.targetRepsMax());
            set.setTargetWeightKg(null);
            set.setTargetTimeSeconds(input.targetTimeSeconds());
            set.setTargetDistanceMeters(input.targetDistanceMeters());
            set.setRestAfterSeconds(input.restAfterSeconds());
            set.setTempo(clean(input.tempo()));
            set.setRpe(input.rpe());
            set.setNotes(clean(input.notes()));
            set.setToFailure(Boolean.TRUE.equals(input.toFailure()));
            exercise.getSets().add(set);
        }
    }

    private TemplateDay copyDay(TemplateDay source, TrainingTemplate target) {
        TemplateDay day = new TemplateDay();
        day.setTemplate(target);
        day.setOrderIndex(source.getOrderIndex());
        day.setName(source.getName());
        day.setNotes(source.getNotes());
        for (TemplateBlock sourceBlock : source.getBlocks()) {
            day.getBlocks().add(copyBlock(sourceBlock, day));
        }
        return day;
    }

    private TemplateBlock copyBlock(TemplateBlock source, TemplateDay target) {
        TemplateBlock block = new TemplateBlock();
        block.setDay(target);
        block.setOrderIndex(source.getOrderIndex());
        block.setTitle(source.getTitle());
        block.setStructuralType(source.getStructuralType());
        block.setPurpose(source.getPurpose());
        block.setTotalDurationSeconds(source.getTotalDurationSeconds());
        block.setTargetRounds(source.getTargetRounds());
        block.setBlockNotes(source.getBlockNotes());
        for (TemplateExercise sourceExercise : source.getExercises()) {
            TemplateExercise exercise = new TemplateExercise();
            exercise.setBlock(block);
            exercise.setExercise(sourceExercise.getExercise());
            exercise.setOrderIndex(sourceExercise.getOrderIndex());
            exercise.setExerciseNotes(sourceExercise.getExerciseNotes());
            for (TemplateExerciseSet sourceSet : sourceExercise.getSets()) {
                TemplateExerciseSet set = new TemplateExerciseSet();
                set.setTemplateExercise(exercise);
                copySet(sourceSet, set);
                exercise.getSets().add(set);
            }
            block.getExercises().add(exercise);
        }
        return block;
    }

    private void copySet(TemplateExerciseSet source, TemplateExerciseSet target) {
        target.setSetNumber(source.getSetNumber());
        target.setSetKind(source.getSetKind());
        target.setTargetReps(source.getTargetReps());
        target.setTargetRepsMin(source.getTargetRepsMin());
        target.setTargetRepsMax(source.getTargetRepsMax());
        target.setTargetWeightKg(source.getTargetWeightKg());
        target.setTargetTimeSeconds(source.getTargetTimeSeconds());
        target.setTargetDistanceMeters(source.getTargetDistanceMeters());
        target.setRestAfterSeconds(source.getRestAfterSeconds());
        target.setTempo(source.getTempo());
        target.setRpe(source.getRpe());
        target.setNotes(source.getNotes());
        target.setToFailure(source.isToFailure());
    }

    private Specification<TrainingTemplate> specification(Long gymId, String search, String sport, String objective, String level, Boolean active) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("gym").get("id"), gymId));
            if (active != null) predicates.add(builder.equal(root.get("active"), active));
            if (StringUtils.hasText(sport)) predicates.add(builder.equal(builder.lower(root.get("sport")), sport.trim().toLowerCase()));
            if (StringUtils.hasText(objective)) predicates.add(builder.equal(builder.lower(root.get("objective")), objective.trim().toLowerCase()));
            if (StringUtils.hasText(level)) predicates.add(builder.equal(builder.lower(root.get("level")), level.trim().toLowerCase()));
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(builder.or(builder.like(builder.lower(root.get("name")), pattern), builder.like(builder.lower(root.get("description")), pattern)));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
