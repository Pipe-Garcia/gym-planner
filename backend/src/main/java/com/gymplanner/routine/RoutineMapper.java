package com.gymplanner.routine;

import com.gymplanner.exercise.ExerciseMapper;
import com.gymplanner.routine.dto.RoutineBlockResponse;
import com.gymplanner.routine.dto.RoutineDayResponse;
import com.gymplanner.routine.dto.RoutineExerciseResponse;
import com.gymplanner.routine.dto.RoutineExerciseSetResponse;
import com.gymplanner.routine.dto.RoutineResponse;
import com.gymplanner.routine.dto.RoutineSummaryResponse;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoutineMapper {
    private final ExerciseMapper exerciseMapper;

    public RoutineResponse toResponse(Routine routine) {
        String studentName = routine.getStudent().getFirstName() + " " + routine.getStudent().getLastName();
        Long sourceTemplateId = routine.getSourceTemplate() == null ? null : routine.getSourceTemplate().getId();
        String sourceTemplateName = routine.getSourceTemplate() == null ? null : routine.getSourceTemplate().getName();
        Long previousRoutineId = routine.getPreviousRoutine() == null ? null : routine.getPreviousRoutine().getId();
        return new RoutineResponse(routine.getId(), routine.getStudent().getId(), studentName, routine.getName(), routine.getObjective(), sourceTemplateId, sourceTemplateName, routine.getStatus(), routine.getAssignedDate(), routine.getFinishedDate(), routine.getFinishedAt(), routine.getGeneralNotes(), routine.getInternalNotes(), routine.getClosureNotes(), previousRoutineId, routine.getCreatedByUser().getId(), routine.getDays().stream().sorted(Comparator.comparingInt(RoutineDay::getOrderIndex)).map(this::toDay).toList(), routine.getCreatedAt(), routine.getUpdatedAt());
    }

    public RoutineSummaryResponse toSummary(Routine routine, long dayCount, long blockCount, long exerciseCount) {
        String studentName = routine.getStudent().getFirstName() + " " + routine.getStudent().getLastName();
        Long sourceTemplateId = routine.getSourceTemplate() == null ? null : routine.getSourceTemplate().getId();
        String sourceTemplateName = routine.getSourceTemplate() == null ? null : routine.getSourceTemplate().getName();
        return new RoutineSummaryResponse(routine.getId(), routine.getStudent().getId(), studentName, routine.getName(), routine.getObjective(), sourceTemplateId, sourceTemplateName, routine.getStatus(), routine.getAssignedDate(), routine.getFinishedDate(), dayCount, blockCount, exerciseCount, routine.getCreatedAt(), routine.getUpdatedAt());
    }

    private RoutineDayResponse toDay(RoutineDay day) {
        return new RoutineDayResponse(day.getId(), day.getOrderIndex(), day.getName(), day.getNotes(), day.getBlocks().stream().sorted(Comparator.comparingInt(RoutineBlock::getOrderIndex)).map(this::toBlock).toList());
    }

    private RoutineBlockResponse toBlock(RoutineBlock block) {
        return new RoutineBlockResponse(block.getId(), block.getOrderIndex(), block.getTitle(), block.getStructuralType(), block.getPurpose(), block.getTotalDurationSeconds(), block.getTargetRounds(), block.getRoundRestSeconds(), block.getBlockNotes(), block.getExercises().stream().sorted(Comparator.comparingInt(RoutineExercise::getOrderIndex)).map(this::toExercise).toList());
    }

    private RoutineExerciseResponse toExercise(RoutineExercise exercise) {
        return new RoutineExerciseResponse(exercise.getId(), exercise.getOrderIndex(), exercise.getExercise().getId(), exercise.getExercise().getName(), exercise.getExercise().isActive(), exercise.getExercise().getDefaultMeasurement(), exerciseMapper.toResponse(exercise.getExercise()), exercise.getExerciseNotes(), exercise.getSets().stream().sorted(Comparator.comparingInt(RoutineExerciseSet::getSetNumber)).map(this::toSet).toList());
    }

    private RoutineExerciseSetResponse toSet(RoutineExerciseSet set) {
        return new RoutineExerciseSetResponse(set.getId(), set.getSetNumber(), set.getSetKind(), set.getTargetReps(), set.getTargetRepsMin(), set.getTargetRepsMax(), set.getTargetWeightKg(), set.getTargetTimeSeconds(), set.getTargetDistanceMeters(), set.getRestAfterSeconds(), set.getTempo(), set.getExecutionCue(), set.getRpe(), set.getNotes(), set.isToFailure());
    }
}
