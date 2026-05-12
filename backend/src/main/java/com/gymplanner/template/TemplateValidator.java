package com.gymplanner.template;

import com.gymplanner.shared.blocks.BlockPurpose;
import com.gymplanner.shared.blocks.BlockStructuralType;
import com.gymplanner.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

@Component
public class TemplateValidator {
    public void validate(TrainingTemplate template) {
        if (template.getDays().isEmpty()) {
            throw new BusinessRuleException("La plantilla debe tener al menos un día");
        }
        for (TemplateDay day : template.getDays()) {
            validateCanonicalSections(day);
            validateBlocks(day);
        }
    }

    private void validateBlocks(TemplateDay day) {
        for (TemplateBlock block : day.getBlocks()) {
            if (block.getExercises().isEmpty()) {
                throw new BusinessRuleException("El bloque '" + block.getTitle() + "' (día '" + day.getName() + "') no tiene ejercicios");
            }
            if (block.getStructuralType() == BlockStructuralType.CIRCUIT
                    && (block.getTotalDurationSeconds() == null || block.getTotalDurationSeconds() <= 0)) {
                throw new BusinessRuleException("El bloque '" + block.getTitle() + "' (día '" + day.getName() + "') es CIRCUIT y requiere duración total");
            }
            for (TemplateExercise exercise : block.getExercises()) {
                if (exercise.getSets().isEmpty()) {
                    throw new BusinessRuleException("El ejercicio '" + exercise.getExercise().getName() + "' en bloque '" + block.getTitle() + "' (día '" + day.getName() + "') no tiene sets");
                }
                exercise.getSets().forEach(set -> {
                    boolean empty = set.getTargetWeightKg() == null
                            && set.getTargetTimeSeconds() == null
                            && set.getTargetDistanceMeters() == null
                            && set.getTargetReps() == null
                            && !set.isToFailure();
                    if (empty) {
                        throw new BusinessRuleException("El set #" + set.getSetNumber() + " no tiene ningun parametro definido");
                    }
                });
            }
        }
    }

    private void validateCanonicalSections(TemplateDay day) {
        boolean hasWarmup = false;
        boolean hasMain = false;
        boolean hasCooldown = false;
        for (TemplateBlock block : day.getBlocks()) {
            BlockPurpose purpose = block.getPurpose();
            if (purpose == null) {
                throw new BusinessRuleException("Bloque sin propósito en día '" + day.getName() + "'");
            }
            if (purpose == BlockPurpose.WARMUP || purpose == BlockPurpose.ACTIVATION) {
                hasWarmup = true;
            }
            if (purpose == BlockPurpose.MAIN_LIFT
                    || purpose == BlockPurpose.ACCESSORY
                    || purpose == BlockPurpose.CONDITIONING
                    || purpose == BlockPurpose.CORE
                    || purpose == BlockPurpose.OTHER) {
                hasMain = true;
            }
            if (purpose == BlockPurpose.COOLDOWN) {
                hasCooldown = true;
            }
        }
        if (!hasWarmup) {
            throw new BusinessRuleException("El día '" + day.getName() + "' debe tener al menos un bloque de calentamiento");
        }
        if (!hasMain) {
            throw new BusinessRuleException("El día '" + day.getName() + "' debe tener al menos un bloque de parte principal");
        }
        if (!hasCooldown) {
            throw new BusinessRuleException("El día '" + day.getName() + "' debe tener al menos un bloque de vuelta a la calma");
        }
    }
}
