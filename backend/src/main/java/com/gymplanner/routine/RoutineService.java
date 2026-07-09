package com.gymplanner.routine;

import com.gymplanner.exercise.ExerciseService;
import com.gymplanner.routine.dto.CreateRoutineFromScratchRequest;
import com.gymplanner.routine.dto.DuplicateRoutineRequest;
import com.gymplanner.routine.dto.FinishRoutineRequest;
import com.gymplanner.routine.dto.RoutineBlockInput;
import com.gymplanner.routine.dto.RoutineDayInput;
import com.gymplanner.routine.dto.RoutineExerciseInput;
import com.gymplanner.routine.dto.RoutineExerciseSetInput;
import com.gymplanner.routine.dto.RoutineResponse;
import com.gymplanner.routine.dto.RoutineSummaryResponse;
import com.gymplanner.routine.dto.UpdateRoutineRequest;
import com.gymplanner.shared.blocks.BlockStructuralType;
import com.gymplanner.shared.blocks.SetKind;
import com.gymplanner.shared.exception.BusinessRuleException;
import com.gymplanner.shared.exception.NotFoundException;
import com.gymplanner.shared.pagination.PageResponse;
import com.gymplanner.student.Student;
import com.gymplanner.student.StudentService;
import com.gymplanner.user.User;
import com.gymplanner.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
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
public class RoutineService {
    private final RoutineRepository routineRepository;
    private final StudentService studentService;
    private final ExerciseService exerciseService;
    private final UserRepository userRepository;
    private final RoutineValidator validator;
    private final RoutineMapper mapper;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public PageResponse<RoutineSummaryResponse> listForStudent(Long gymId, Long studentId, String statusesCsv, Pageable pageable) {
        studentService.getEntity(gymId, studentId);
        Page<Routine> page = routineRepository.findAll(specification(gymId, studentId, parseStatuses(statusesCsv)), pageable);
        return new PageResponse<>(page.map(r -> mapper.toSummary(r, routineRepository.countDays(r.getId()), routineRepository.countBlocks(r.getId()), routineRepository.countExercises(r.getId()))).getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PageResponse<RoutineSummaryResponse> list(Long gymId, String statusesCsv, String q, LocalDate dateFrom, LocalDate dateTo, String sport, String level, Pageable pageable) {
        Page<Routine> page = routineRepository.findAll(specification(gymId, parseStatusesOrAll(statusesCsv), q, dateFrom, dateTo, sport, level), pageable);
        return new PageResponse<>(page.map(r -> mapper.toSummary(r, routineRepository.countDays(r.getId()), routineRepository.countBlocks(r.getId()), routineRepository.countExercises(r.getId()))).getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public RoutineResponse getActive(Long gymId, Long studentId) {
        return mapper.toResponse(routineRepository.findFirstByStudentIdAndStudentGymIdAndStatus(studentId, gymId, RoutineStatus.ACTIVE).orElseThrow(() -> new NotFoundException("Routine not found.")));
    }

    @Transactional(readOnly = true)
    public RoutineResponse get(Long gymId, Long id) {
        return mapper.toResponse(getFull(gymId, id));
    }

    @Transactional
    public RoutineResponse duplicate(Long gymId, Long userId, Long id, DuplicateRoutineRequest request) {
        Routine original = getFull(gymId, id);
        Student targetStudent = studentService.getEntity(gymId, request.targetStudentId());
        RoutineStatus newStatus = request.status() == null ? RoutineStatus.DRAFT : request.status();
        User currentUser = userRepository.getReferenceById(userId);

        Routine copy = new Routine();
        copy.setStudent(targetStudent);
        copy.setCreatedByUser(currentUser);
        copy.setSourceTemplate(original.getSourceTemplate());
        copy.setName(StringUtils.hasText(request.name()) ? request.name().trim() : original.getName() + " (copia)");
        copy.setObjective(clean(original.getObjective()));
        copy.setStatus(newStatus);
        copy.setAssignedDate(request.assignedDate() == null ? LocalDate.now() : request.assignedDate());
        copy.setGeneralNotes(clean(original.getGeneralNotes()));
        copy.setInternalNotes(clean(original.getInternalNotes()));
        copy.getDays().addAll(copyDays(original, copy));

        if (newStatus == RoutineStatus.ACTIVE) {
            finishPreviousActive(gymId, targetStudent.getId(), null, currentUser);
        }
        validator.validate(copy);
        return mapper.toResponse(routineRepository.save(copy));
    }

    @Transactional
    public RoutineResponse createFromScratch(Long gymId, Long userId, CreateRoutineFromScratchRequest request) {
        Student student = studentService.getEntity(gymId, request.studentId());
        User currentUser = userRepository.getReferenceById(userId);
        Routine routine = new Routine();
        routine.setStudent(student);
        routine.setCreatedByUser(currentUser);
        routine.setName(request.name().trim());
        routine.setObjective(clean(request.objective()));
        routine.setStatus(request.status() == null ? RoutineStatus.ACTIVE : request.status());
        routine.setAssignedDate(request.assignedDate() == null ? LocalDate.now() : request.assignedDate());
        routine.setGeneralNotes(clean(request.generalNotes()));
        routine.setInternalNotes(clean(request.internalNotes()));
        routine.getDays().addAll(mapDays(gymId, request.days(), routine));
        if (routine.getStatus() == RoutineStatus.ACTIVE) {
            finishPreviousActive(gymId, student.getId(), null, currentUser);
        }
        validator.validate(routine);
        return mapper.toResponse(routineRepository.save(routine));
    }

    @Transactional
    public RoutineResponse update(Long gymId, Long id, UpdateRoutineRequest request) {
        Routine routine = getFull(gymId, id);
        if (routine.getStatus() == RoutineStatus.FINISHED || routine.getStatus() == RoutineStatus.ARCHIVED) {
            throw new BusinessRuleException("No se puede editar una rutina finalizada o archivada. Duplicala o crea una nueva.");
        }
        RoutineStatus newStatus = request.status() == null ? routine.getStatus() : request.status();
        routine.setName(request.name().trim());
        routine.setObjective(clean(request.objective()));
        routine.setStatus(newStatus);
        routine.setAssignedDate(request.assignedDate() == null ? routine.getAssignedDate() : request.assignedDate());
        routine.setFinishedDate(request.finishedDate());
        routine.setGeneralNotes(clean(request.generalNotes()));
        routine.setInternalNotes(clean(request.internalNotes()));
        routine.getDays().clear();
        entityManager.flush();
        routine.getDays().addAll(mapDays(gymId, request.days(), routine));
        if (newStatus == RoutineStatus.ACTIVE) {
            finishPreviousActive(gymId, routine.getStudent().getId(), routine.getId());
        }
        validator.validate(routine);
        return mapper.toResponse(routineRepository.save(routine));
    }

    @Transactional
    public RoutineResponse finishRoutine(Long gymId, Long userId, Long id, FinishRoutineRequest request) {
        Routine routine = getFull(gymId, id);
        if (routine.getStatus() == RoutineStatus.FINISHED || routine.getStatus() == RoutineStatus.ARCHIVED) {
            throw new BusinessRuleException("Esta rutina ya está finalizada o archivada.");
        }
        if (routine.getStatus() != RoutineStatus.ACTIVE && routine.getStatus() != RoutineStatus.DRAFT) {
            throw new BusinessRuleException("Solo se puede finalizar una rutina activa o borrador.");
        }
        finishRoutineEntity(routine, userRepository.getReferenceById(userId), Instant.now(), request == null ? null : request.closureNotes());
        return mapper.toResponse(routine);
    }

    @Transactional
    public RoutineResponse archiveRoutine(Long gymId, Long userId, Long id) {
        Routine routine = getFull(gymId, id);
        if (routine.getStatus() == RoutineStatus.ARCHIVED) {
            throw new BusinessRuleException("Ya está archivada.");
        }
        if (routine.getStatus() == RoutineStatus.DRAFT) {
            throw new BusinessRuleException("No se puede archivar un borrador. Eliminalo.");
        }
        if (routine.getStatus() == RoutineStatus.ACTIVE) {
            Instant now = Instant.now();
            routine.setFinishedAt(routine.getFinishedAt() == null ? now : routine.getFinishedAt());
            routine.setFinishedDate(routine.getFinishedDate() == null ? LocalDate.now() : routine.getFinishedDate());
            if (routine.getFinishedByUser() == null) {
                routine.setFinishedByUser(userRepository.getReferenceById(userId));
            }
        }
        routine.setStatus(RoutineStatus.ARCHIVED);
        return mapper.toResponse(routine);
    }

    @Transactional
    public RoutineResponse activateRoutine(Long gymId, Long userId, Long id) {
        Routine routine = getFull(gymId, id);
        if (routine.getStatus() != RoutineStatus.DRAFT) {
            throw new BusinessRuleException("Solo se puede activar una rutina en borrador.");
        }
        User currentUser = userRepository.getReferenceById(userId);
        routine.setStatus(RoutineStatus.ACTIVE);
        validator.validate(routine);
        finishPreviousActive(gymId, routine.getStudent().getId(), routine.getId(), currentUser);
        return mapper.toResponse(routine);
    }

    @Transactional
    public void delete(Long gymId, Long id) {
        Routine routine = getFull(gymId, id);
        if (routine.getStatus() != RoutineStatus.DRAFT) {
            throw new BusinessRuleException("Solo se pueden eliminar rutinas en borrador. Para finalizar o archivar, usá los endpoints correspondientes.");
        }
        routineRepository.delete(routine);
    }

    @Transactional(readOnly = true)
    public Routine getFull(Long gymId, Long id) {
        return routineRepository.findByIdWithFullStructure(id, gymId).orElseThrow(() -> new NotFoundException("Routine not found."));
    }

    void finishPreviousActive(Long gymId, Long studentId) {
        finishPreviousActive(gymId, studentId, null, null);
    }

    void finishPreviousActive(Long gymId, Long studentId, Long exceptRoutineId) {
        finishPreviousActive(gymId, studentId, exceptRoutineId, null);
    }

    void finishPreviousActive(Long gymId, Long studentId, Long exceptRoutineId, User finishedByUser) {
        Instant now = Instant.now();
        routineRepository.findFirstByStudentIdAndStudentGymIdAndStatus(studentId, gymId, RoutineStatus.ACTIVE)
                .filter(r -> exceptRoutineId == null || !r.getId().equals(exceptRoutineId))
                .ifPresent(r -> {
                    r.setStatus(RoutineStatus.FINISHED);
                    r.setFinishedAt(now);
                    r.setFinishedDate(LocalDate.now());
                    r.setFinishedByUser(finishedByUser);
                });
    }

    List<RoutineDay> mapDays(Long gymId, List<RoutineDayInput> inputs, Routine routine) {
        List<RoutineDay> days = new ArrayList<>();
        if (inputs == null) return days;
        int dayIndex = 1;
        for (RoutineDayInput input : inputs) {
            RoutineDay day = new RoutineDay();
            day.setRoutine(routine);
            day.setOrderIndex(dayIndex++);
            day.setName(input.name().trim());
            day.setNotes(clean(input.notes()));
            day.getBlocks().addAll(mapBlocks(gymId, input.blocks(), day));
            days.add(day);
        }
        return days;
    }

    List<RoutineDay> copyDays(Routine original, Routine copy) {
        return original.getDays().stream()
                .map(originalDay -> {
                    RoutineDay day = new RoutineDay();
                    day.setRoutine(copy);
                    day.setOrderIndex(originalDay.getOrderIndex());
                    day.setName(originalDay.getName());
                    day.setNotes(clean(originalDay.getNotes()));
                    day.getBlocks().addAll(copyBlocks(originalDay, day));
                    return day;
                })
                .toList();
    }

    private List<RoutineBlock> copyBlocks(RoutineDay originalDay, RoutineDay copyDay) {
        return originalDay.getBlocks().stream()
                .map(originalBlock -> {
                    RoutineBlock block = new RoutineBlock();
                    block.setDay(copyDay);
                    block.setOrderIndex(originalBlock.getOrderIndex());
                    block.setTitle(originalBlock.getTitle());
                    block.setStructuralType(originalBlock.getStructuralType());
                    block.setPurpose(originalBlock.getPurpose());
                    block.setTotalDurationSeconds(originalBlock.getTotalDurationSeconds());
                    block.setTargetRounds(originalBlock.getTargetRounds());
                    block.setRoundRestSeconds(originalBlock.getRoundRestSeconds());
                    block.setBlockNotes(clean(originalBlock.getBlockNotes()));
                    block.getExercises().addAll(copyExercises(originalBlock, block));
                    return block;
                })
                .toList();
    }

    private List<RoutineExercise> copyExercises(RoutineBlock originalBlock, RoutineBlock copyBlock) {
        return originalBlock.getExercises().stream()
                .map(originalExercise -> {
                    RoutineExercise exercise = new RoutineExercise();
                    exercise.setBlock(copyBlock);
                    exercise.setExercise(originalExercise.getExercise());
                    exercise.setOrderIndex(originalExercise.getOrderIndex());
                    exercise.setExerciseNotes(clean(originalExercise.getExerciseNotes()));
                    exercise.getSets().addAll(copySets(originalExercise, exercise));
                    return exercise;
                })
                .toList();
    }

    private List<RoutineExerciseSet> copySets(RoutineExercise originalExercise, RoutineExercise copyExercise) {
        return originalExercise.getSets().stream()
                .map(originalSet -> {
                    RoutineExerciseSet set = new RoutineExerciseSet();
                    set.setRoutineExercise(copyExercise);
                    set.setSetNumber(originalSet.getSetNumber());
                    set.setSetKind(originalSet.getSetKind());
                    set.setTargetReps(originalSet.getTargetReps());
                    set.setTargetRepsMin(originalSet.getTargetRepsMin());
                    set.setTargetRepsMax(originalSet.getTargetRepsMax());
                    set.setTargetWeightKg(originalSet.getTargetWeightKg());
                    set.setTargetTimeSeconds(originalSet.getTargetTimeSeconds());
                    set.setTargetDistanceMeters(originalSet.getTargetDistanceMeters());
                    set.setRestAfterSeconds(originalSet.getRestAfterSeconds());
                    set.setTempo(clean(originalSet.getTempo()));
                    set.setExecutionCue(clean(originalSet.getExecutionCue()));
                    set.setRpe(originalSet.getRpe());
                    set.setNotes(clean(originalSet.getNotes()));
                    set.setToFailure(originalSet.isToFailure());
                    return set;
                })
                .toList();
    }

    List<RoutineBlock> mapBlocks(Long gymId, List<RoutineBlockInput> inputs, RoutineDay day) {
        List<RoutineBlock> blocks = new ArrayList<>();
        if (inputs == null) return blocks;
        int blockIndex = 1;
        for (RoutineBlockInput input : inputs) {
            RoutineBlock block = new RoutineBlock();
            block.setDay(day);
            block.setOrderIndex(blockIndex++);
            block.setTitle(input.title().trim());
            block.setStructuralType(input.structuralType());
            block.setPurpose(input.purpose());
            block.setTotalDurationSeconds(input.totalDurationSeconds());
            block.setTargetRounds(input.targetRounds());
            block.setRoundRestSeconds(input.roundRestSeconds());
            block.setBlockNotes(clean(input.blockNotes()));
            int exerciseIndex = 1;
            if (input.exercises() != null) {
                for (RoutineExerciseInput exerciseInput : input.exercises()) {
                    RoutineExercise exercise = new RoutineExercise();
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

    private void addSets(RoutineExercise exercise, List<RoutineExerciseSetInput> inputs, BlockStructuralType structuralType) {
        if (inputs == null) return;
        List<RoutineExerciseSetInput> effectiveInputs = inputs;
        if (structuralType == BlockStructuralType.CIRCUIT && inputs.size() > 1) {
            log.warn("Routine circuit exercise {} received {} sets; keeping only the first.", exercise.getExercise().getId(), inputs.size());
            effectiveInputs = inputs.subList(0, 1);
        }
        int setNumber = 1;
        for (RoutineExerciseSetInput input : effectiveInputs) {
            RoutineExerciseSet set = new RoutineExerciseSet();
            set.setRoutineExercise(exercise);
            set.setSetNumber(setNumber++);
            set.setSetKind(input.setKind() == null ? SetKind.NORMAL : input.setKind());
            set.setTargetReps(input.targetReps());
            set.setTargetRepsMin(input.targetRepsMin());
            set.setTargetRepsMax(input.targetRepsMax());
            set.setTargetWeightKg(input.targetWeightKg());
            set.setTargetTimeSeconds(input.targetTimeSeconds());
            set.setTargetDistanceMeters(input.targetDistanceMeters());
            set.setRestAfterSeconds(input.restAfterSeconds());
            set.setTempo(clean(input.tempo()));
            set.setExecutionCue(clean(input.executionCue()));
            set.setRpe(input.rpe());
            set.setNotes(clean(input.notes()));
            set.setToFailure(Boolean.TRUE.equals(input.toFailure()));
            exercise.getSets().add(set);
        }
    }

    private Specification<Routine> specification(Long gymId, Long studentId, List<RoutineStatus> statuses) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("student").get("gym").get("id"), gymId));
            predicates.add(builder.equal(root.get("student").get("id"), studentId));
            if (!statuses.isEmpty()) predicates.add(root.get("status").in(statuses));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<Routine> specification(Long gymId, List<RoutineStatus> statuses, String q, LocalDate dateFrom, LocalDate dateTo, String sport, String level) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("student").get("gym").get("id"), gymId));
            if (!statuses.isEmpty()) predicates.add(root.get("status").in(statuses));
            if (dateFrom != null) predicates.add(builder.greaterThanOrEqualTo(root.get("assignedDate"), dateFrom));
            if (dateTo != null) predicates.add(builder.lessThanOrEqualTo(root.get("assignedDate"), dateTo));
            if (StringUtils.hasText(q)) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("student").get("firstName")), pattern),
                        builder.like(builder.lower(root.get("student").get("lastName")), pattern)
                ));
            }
            if (StringUtils.hasText(sport)) {
                predicates.add(builder.equal(builder.lower(root.get("student").get("sport")), sport.trim().toLowerCase()));
            }
            if (StringUtils.hasText(level)) {
                predicates.add(builder.equal(builder.lower(root.get("student").get("level")), level.trim().toLowerCase()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private List<RoutineStatus> parseStatuses(String statusesCsv) {
        if (!StringUtils.hasText(statusesCsv)) return List.of(RoutineStatus.ACTIVE, RoutineStatus.DRAFT, RoutineStatus.FINISHED);
        return Arrays.stream(statusesCsv.split(",")).map(String::trim).filter(StringUtils::hasText).map(RoutineStatus::valueOf).toList();
    }

    private List<RoutineStatus> parseStatusesOrAll(String statusesCsv) {
        if (!StringUtils.hasText(statusesCsv)) return List.of();
        return Arrays.stream(statusesCsv.split(",")).map(String::trim).filter(StringUtils::hasText).map(RoutineStatus::valueOf).toList();
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    void finishRoutineEntity(Routine routine, User finishedByUser, Instant now, String closureNotes) {
        routine.setStatus(RoutineStatus.FINISHED);
        routine.setFinishedAt(now);
        routine.setFinishedDate(LocalDate.now());
        routine.setFinishedByUser(finishedByUser);
        routine.setClosureNotes(clean(closureNotes));
    }
}
