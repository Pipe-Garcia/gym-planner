package com.gymplanner.student.history;

import com.gymplanner.exercise.ExerciseRepository;
import com.gymplanner.routine.Routine;
import com.gymplanner.routine.RoutineBlock;
import com.gymplanner.routine.RoutineDay;
import com.gymplanner.routine.RoutineExercise;
import com.gymplanner.routine.RoutineExerciseSet;
import com.gymplanner.routine.RoutineStatus;
import com.gymplanner.shared.blocks.BlockStructuralType;
import com.gymplanner.shared.exception.NotFoundException;
import com.gymplanner.shared.pagination.PageResponse;
import com.gymplanner.student.Student;
import com.gymplanner.student.StudentRepository;
import com.gymplanner.student.history.StudentHistoryRepository.ExerciseAppearanceProjection;
import com.gymplanner.student.history.StudentHistoryRepository.IdCountProjection;
import com.gymplanner.student.history.dto.StudentExerciseHistoryItemResponse;
import com.gymplanner.student.history.dto.StudentExerciseOccurrenceResponse;
import com.gymplanner.student.history.dto.StudentExerciseOccurrenceSetResponse;
import com.gymplanner.student.history.dto.StudentHistorySummaryResponse;
import com.gymplanner.student.history.dto.StudentRoutineTimelineItemResponse;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StudentHistoryService {
    private static final Collection<RoutineStatus> INCLUDED_STATUSES = List.of(
            RoutineStatus.ACTIVE,
            RoutineStatus.FINISHED,
            RoutineStatus.ARCHIVED);

    private final StudentRepository studentRepository;
    private final ExerciseRepository exerciseRepository;
    private final StudentHistoryRepository studentHistoryRepository;

    @Transactional(readOnly = true)
    public StudentHistorySummaryResponse getSummary(Long gymId, Long studentId) {
        Student student = getStudent(gymId, studentId);
        Routine activeRoutine = studentHistoryRepository.findActiveRoutines(gymId, studentId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElse(null);

        return new StudentHistorySummaryResponse(
                student.getId(),
                fullName(student),
                studentHistoryRepository.countIncludedRoutines(gymId, studentId, INCLUDED_STATUSES),
                activeRoutine == null ? null : activeRoutine.getId(),
                activeRoutine == null ? null : activeRoutine.getName(),
                activeRoutine == null ? null : activeRoutine.getAssignedDate(),
                activeRoutine == null ? null : durationFrom(activeRoutine.getAssignedDate(), LocalDate.now()),
                studentHistoryRepository.countDistinctExercises(gymId, studentId, INCLUDED_STATUSES),
                studentHistoryRepository.findTrainingSince(gymId, studentId, INCLUDED_STATUSES));
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentRoutineTimelineItemResponse> getTimeline(Long gymId, Long studentId, int page, int size) {
        getStudent(gymId, studentId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("assignedDate"),
                Sort.Order.desc("id")));
        Page<Routine> routines = studentHistoryRepository.findTimelineRoutines(gymId, studentId, INCLUDED_STATUSES, pageable);
        List<Long> routineIds = routines.getContent().stream().map(Routine::getId).toList();
        Map<Long, Long> daysByRoutine = countMap(studentHistoryRepository.countDaysByRoutineIds(routineIds));
        Map<Long, Long> blocksByRoutine = countMap(studentHistoryRepository.countBlocksByRoutineIds(routineIds));
        Map<Long, Long> exercisesByRoutine = countMap(studentHistoryRepository.countExercisesByRoutineIds(routineIds));

        List<StudentRoutineTimelineItemResponse> content = routines.getContent().stream()
                .map(routine -> toTimelineItem(routine, daysByRoutine, blocksByRoutine, exercisesByRoutine))
                .toList();
        return page(content, routines);
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentExerciseHistoryItemResponse> getExerciseHistory(Long gymId, Long studentId, String search, int page, int size) {
        getStudent(gymId, studentId);
        String normalizedSearch = normalizeSearch(search);
        Pageable pageable = PageRequest.of(page, size);
        Page<Long> exerciseIds = normalizedSearch == null
                ? studentHistoryRepository.findExerciseHistoryExerciseIds(gymId, studentId, INCLUDED_STATUSES, pageable)
                : studentHistoryRepository.findExerciseHistoryExerciseIdsBySearch(gymId, studentId, INCLUDED_STATUSES, normalizedSearch, pageable);
        if (exerciseIds.isEmpty()) {
            return page(List.of(), exerciseIds);
        }

        Map<Long, List<ExerciseAppearanceProjection>> appearancesByExercise = studentHistoryRepository.findExerciseAppearances(
                        gymId,
                        studentId,
                        INCLUDED_STATUSES,
                        exerciseIds.getContent())
                .stream()
                .collect(Collectors.groupingBy(ExerciseAppearanceProjection::getExerciseId));

        List<StudentExerciseHistoryItemResponse> content = exerciseIds.getContent().stream()
                .map(id -> toExerciseHistoryItem(id, appearancesByExercise.getOrDefault(id, List.of())))
                .filter(Objects::nonNull)
                .toList();
        return page(content, exerciseIds);
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentExerciseOccurrenceResponse> getExerciseOccurrences(Long gymId, Long studentId, Long exerciseId, int page, int size) {
        getStudent(gymId, studentId);
        exerciseRepository.findByIdAndGymId(exerciseId, gymId)
                .orElseThrow(() -> new NotFoundException("Ejercicio no encontrado"));
        Page<Long> ids = studentHistoryRepository.findOccurrenceIds(
                gymId,
                studentId,
                exerciseId,
                INCLUDED_STATUSES,
                PageRequest.of(page, size));
        if (ids.isEmpty()) {
            return page(List.of(), ids);
        }

        Map<Long, RoutineExercise> byId = studentHistoryRepository.findOccurrencesWithContextAndSets(ids.getContent())
                .stream()
                .collect(Collectors.toMap(RoutineExercise::getId, Function.identity()));
        List<StudentExerciseOccurrenceResponse> content = ids.getContent().stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(this::toOccurrence)
                .toList();
        return page(content, ids);
    }

    private Student getStudent(Long gymId, Long studentId) {
        return studentRepository.findByIdAndGymId(studentId, gymId)
                .orElseThrow(() -> new NotFoundException("Alumno no encontrado"));
    }

    private String fullName(Student student) {
        return (student.getFirstName() + " " + student.getLastName()).trim();
    }

    private String normalizeSearch(String search) {
        return StringUtils.hasText(search) ? search.trim().toLowerCase() : null;
    }

    private StudentRoutineTimelineItemResponse toTimelineItem(Routine routine, Map<Long, Long> daysByRoutine,
            Map<Long, Long> blocksByRoutine, Map<Long, Long> exercisesByRoutine) {
        LocalDate endDate = routine.getFinishedDate() == null ? LocalDate.now() : routine.getFinishedDate();
        return new StudentRoutineTimelineItemResponse(
                routine.getId(),
                routine.getName(),
                routine.getStatus(),
                routine.getAssignedDate(),
                routine.getFinishedDate(),
                durationFrom(routine.getAssignedDate(), endDate),
                daysByRoutine.getOrDefault(routine.getId(), 0L),
                blocksByRoutine.getOrDefault(routine.getId(), 0L),
                exercisesByRoutine.getOrDefault(routine.getId(), 0L),
                routine.getSourceTemplate() == null ? null : routine.getSourceTemplate().getId(),
                routine.getSourceTemplate() == null ? null : routine.getSourceTemplate().getName(),
                routine.getClosureNotes());
    }

    private StudentExerciseHistoryItemResponse toExerciseHistoryItem(Long exerciseId, List<ExerciseAppearanceProjection> appearances) {
        if (appearances.isEmpty()) {
            return null;
        }
        List<ExerciseAppearanceProjection> ordered = appearances.stream()
                .sorted(Comparator.comparing(ExerciseAppearanceProjection::getEffectiveDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ExerciseAppearanceProjection::getRoutineId, Comparator.reverseOrder()))
                .toList();
        ExerciseAppearanceProjection latest = ordered.getFirst();
        List<BlockStructuralType> structuralTypes = appearances.stream()
                .map(ExerciseAppearanceProjection::getStructuralType)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(BlockStructuralType.class)))
                .stream()
                .sorted(Comparator.comparing(Enum::name))
                .toList();

        return new StudentExerciseHistoryItemResponse(
                exerciseId,
                latest.getExerciseName(),
                latest.getEffectiveDate(),
                latest.getRoutineId(),
                latest.getRoutineName(),
                appearances.size(),
                structuralTypes,
                latest.getStructuralType());
    }

    private StudentExerciseOccurrenceResponse toOccurrence(RoutineExercise routineExercise) {
        RoutineBlock block = routineExercise.getBlock();
        RoutineDay day = block.getDay();
        Routine routine = day.getRoutine();
        List<StudentExerciseOccurrenceSetResponse> sets = routineExercise.getSets().stream()
                .sorted(Comparator.comparingInt(RoutineExerciseSet::getSetNumber))
                .map(this::toSet)
                .toList();

        return new StudentExerciseOccurrenceResponse(
                routine.getId(),
                routine.getName(),
                routine.getStatus(),
                routine.getAssignedDate(),
                routine.getFinishedDate(),
                routine.getFinishedDate() == null ? routine.getAssignedDate() : routine.getFinishedDate(),
                day.getOrderIndex(),
                day.getName(),
                block.getTitle(),
                block.getStructuralType(),
                block.getPurpose(),
                routineExercise.getExerciseNotes(),
                routineExercise.getExercise().getDefaultMeasurement(),
                sets);
    }

    private StudentExerciseOccurrenceSetResponse toSet(RoutineExerciseSet set) {
        return new StudentExerciseOccurrenceSetResponse(
                set.getSetNumber(),
                set.getSetKind(),
                set.getTargetReps(),
                set.getTargetRepsMin(),
                set.getTargetRepsMax(),
                set.getTargetWeightKg(),
                set.getTargetTimeSeconds(),
                set.getTargetDistanceMeters(),
                set.getRestAfterSeconds(),
                set.getTempo(),
                set.getRpe(),
                set.isToFailure(),
                set.getNotes());
    }

    private Long durationFrom(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    private Map<Long, Long> countMap(List<IdCountProjection> rows) {
        return rows.stream().collect(Collectors.toMap(IdCountProjection::getId, IdCountProjection::getCount));
    }

    private <T> PageResponse<T> page(List<T> content, Page<?> page) {
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
