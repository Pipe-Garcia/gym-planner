package com.gymplanner.template;

import com.gymplanner.template.dto.TemplateBlockResponse;
import com.gymplanner.template.dto.TemplateDayResponse;
import com.gymplanner.template.dto.TemplateExerciseResponse;
import com.gymplanner.template.dto.TemplateExerciseSetResponse;
import com.gymplanner.template.dto.TemplateResponse;
import com.gymplanner.template.dto.TemplateSummaryResponse;
import java.util.Comparator;
import org.springframework.stereotype.Component;

@Component
public class TemplateMapper {
    public TemplateResponse toResponse(TrainingTemplate template) {
        return new TemplateResponse(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getSport(),
                template.getObjective(),
                template.getLevel(),
                template.getEstimatedDurationMinutes(),
                template.getGeneralNotes(),
                template.isActive(),
                template.getCreatedByUser().getId(),
                template.getDays().stream().sorted(Comparator.comparingInt(TemplateDay::getOrderIndex)).map(this::toDay).toList(),
                template.getCreatedAt(),
                template.getUpdatedAt());
    }

    public TemplateSummaryResponse toSummary(TrainingTemplate template, long dayCount, long blockCount, long exerciseCount) {
        return new TemplateSummaryResponse(template.getId(), template.getName(), template.getDescription(), template.getSport(), template.getObjective(), template.getLevel(), template.getEstimatedDurationMinutes(), template.isActive(), dayCount, blockCount, exerciseCount, template.getCreatedAt(), template.getUpdatedAt());
    }

    private TemplateDayResponse toDay(TemplateDay day) {
        return new TemplateDayResponse(day.getId(), day.getOrderIndex(), day.getName(), day.getNotes(), day.getBlocks().stream().sorted(Comparator.comparingInt(TemplateBlock::getOrderIndex)).map(this::toBlock).toList());
    }

    private TemplateBlockResponse toBlock(TemplateBlock block) {
        return new TemplateBlockResponse(block.getId(), block.getOrderIndex(), block.getTitle(), block.getStructuralType(), block.getPurpose(), block.getTotalDurationSeconds(), block.getTargetRounds(), block.getBlockNotes(), block.getExercises().stream().sorted(Comparator.comparingInt(TemplateExercise::getOrderIndex)).map(this::toExercise).toList());
    }

    private TemplateExerciseResponse toExercise(TemplateExercise exercise) {
        return new TemplateExerciseResponse(exercise.getId(), exercise.getOrderIndex(), exercise.getExercise().getId(), exercise.getExercise().getName(), exercise.getExercise().isActive(), exercise.getExercise().getDefaultMeasurement(), exercise.getExerciseNotes(), exercise.getSets().stream().sorted(Comparator.comparingInt(TemplateExerciseSet::getSetNumber)).map(this::toSet).toList());
    }

    private TemplateExerciseSetResponse toSet(TemplateExerciseSet set) {
        return new TemplateExerciseSetResponse(set.getId(), set.getSetNumber(), set.getSetKind(), set.getTargetReps(), set.getTargetRepsMin(), set.getTargetRepsMax(), set.getTargetWeightKg(), set.getTargetTimeSeconds(), set.getTargetDistanceMeters(), set.getRestAfterSeconds(), set.getTempo(), set.getExecutionCue(), set.getRpe(), set.getNotes(), set.isToFailure());
    }
}
