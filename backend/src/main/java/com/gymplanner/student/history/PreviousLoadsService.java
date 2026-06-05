package com.gymplanner.student.history;

import com.gymplanner.exercise.Exercise;
import com.gymplanner.exercise.ExerciseRepository;
import com.gymplanner.routine.Routine;
import com.gymplanner.routine.RoutineBlock;
import com.gymplanner.routine.RoutineDay;
import com.gymplanner.routine.RoutineExercise;
import com.gymplanner.routine.RoutineExerciseSet;
import com.gymplanner.routine.RoutineStatus;
import com.gymplanner.shared.blocks.BlockStructuralType;
import com.gymplanner.shared.exception.NotFoundException;
import com.gymplanner.student.StudentRepository;
import com.gymplanner.student.history.dto.PreviousLoadOccurrence;
import com.gymplanner.student.history.dto.PreviousLoadSet;
import com.gymplanner.student.history.dto.PreviousLoadsResponse;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PreviousLoadsService {
    private static final int DEFAULT_LIMIT = 1;
    private static final int MAX_LIMIT = 3;
    private static final Collection<RoutineStatus> INCLUDED_STATUSES = List.of(
            RoutineStatus.ACTIVE,
            RoutineStatus.FINISHED,
            RoutineStatus.ARCHIVED);

    private final StudentRepository studentRepository;
    private final ExerciseRepository exerciseRepository;
    private final PreviousLoadsRepository previousLoadsRepository;

    @Transactional(readOnly = true)
    public PreviousLoadsResponse getPreviousLoads(Long gymId, Long studentId, Long exerciseId, Long excludeRoutineId, Integer limit) {
        return getPreviousLoads(gymId, studentId, exerciseId, excludeRoutineId, null, false, limit);
    }

    @Transactional(readOnly = true)
    public PreviousLoadsResponse getPreviousLoads(Long gymId, Long studentId, Long exerciseId, Long excludeRoutineId, BlockStructuralType structuralType, Integer limit) {
        return getPreviousLoads(gymId, studentId, exerciseId, excludeRoutineId, structuralType, false, limit);
    }

    @Transactional(readOnly = true)
    public PreviousLoadsResponse getPreviousLoads(Long gymId, Long studentId, Long exerciseId, Long excludeRoutineId, BlockStructuralType structuralType, boolean includeFallback, Integer limit) {
        studentRepository.findByIdAndGymId(studentId, gymId)
                .orElseThrow(() -> new NotFoundException("Alumno no encontrado"));
        Exercise exercise = exerciseRepository.findByIdAndGymId(exerciseId, gymId)
                .orElseThrow(() -> new NotFoundException("Ejercicio no encontrado"));

        int normalizedLimit = normalizedLimit(limit);
        List<Long> ids = findIds(gymId, studentId, exerciseId, excludeRoutineId, structuralType, normalizedLimit);
        PreviousLoadsMatchType matchType = ids.isEmpty()
                ? PreviousLoadsMatchType.NONE
                : PreviousLoadsMatchType.SAME_STRUCTURAL_TYPE;

        if (ids.isEmpty() && structuralType != null && includeFallback) {
            ids = findIds(gymId, studentId, exerciseId, excludeRoutineId, null, normalizedLimit);
            matchType = ids.isEmpty()
                    ? PreviousLoadsMatchType.NONE
                    : PreviousLoadsMatchType.DIFFERENT_STRUCTURAL_TYPE;
        }

        if (ids.isEmpty()) {
            return new PreviousLoadsResponse(exercise.getId(), exercise.getName(), false, PreviousLoadsMatchType.NONE, structuralType, List.of());
        }

        Map<Long, RoutineExercise> byId = previousLoadsRepository.findByIdsWithSets(ids).stream()
                .collect(Collectors.toMap(RoutineExercise::getId, Function.identity()));
        List<PreviousLoadOccurrence> occurrences = ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(this::toOccurrence)
                .toList();

        return new PreviousLoadsResponse(exercise.getId(), exercise.getName(), !occurrences.isEmpty(),
                occurrences.isEmpty() ? PreviousLoadsMatchType.NONE : matchType,
                structuralType,
                occurrences);
    }

    private List<Long> findIds(Long gymId, Long studentId, Long exerciseId, Long excludeRoutineId, BlockStructuralType structuralType, int limit) {
        return previousLoadsRepository.findPreviousRoutineExerciseIds(
                gymId,
                studentId,
                exerciseId,
                excludeRoutineId,
                structuralType,
                INCLUDED_STATUSES,
                PageRequest.of(0, limit));
    }

    private int normalizedLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private PreviousLoadOccurrence toOccurrence(RoutineExercise routineExercise) {
        RoutineBlock block = routineExercise.getBlock();
        RoutineDay day = block.getDay();
        Routine routine = day.getRoutine();
        List<PreviousLoadSet> sets = routineExercise.getSets().stream()
                .sorted(Comparator.comparingInt(RoutineExerciseSet::getSetNumber))
                .map(this::toSet)
                .toList();

        return new PreviousLoadOccurrence(
                routine.getId(),
                routine.getName(),
                routine.getStatus(),
                routine.getAssignedDate(),
                routine.getFinishedDate(),
                day.getOrderIndex(),
                day.getName(),
                block.getTitle(),
                block.getStructuralType(),
                block.getPurpose(),
                routineExercise.getExerciseNotes(),
                routineExercise.getExercise().getDefaultMeasurement(),
                sets);
    }

    private PreviousLoadSet toSet(RoutineExerciseSet set) {
        return new PreviousLoadSet(
                set.getSetNumber(),
                set.getSetKind(),
                set.getTargetReps(),
                set.getTargetRepsMin(),
                set.getTargetRepsMax(),
                set.getTargetWeightKg(),
                set.getTargetTimeSeconds(),
                set.getTargetDistanceMeters(),
                set.getRestAfterSeconds(),
                set.getRpe(),
                set.isToFailure(),
                set.getExecutionCue());
    }
}
