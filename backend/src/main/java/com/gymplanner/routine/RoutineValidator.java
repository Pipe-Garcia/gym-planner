package com.gymplanner.routine;

import com.gymplanner.shared.blocks.BlockPurpose;
import com.gymplanner.shared.blocks.BlockStructuralType;
import com.gymplanner.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

@Component
public class RoutineValidator {
    public void validate(Routine routine) {
        if (routine.getStatus() == RoutineStatus.DRAFT) {
            return;
        }
        if (routine.getDays().isEmpty()) {
            throw new BusinessRuleException("La rutina debe tener al menos un día");
        }
        for (RoutineDay day : routine.getDays()) {
            validateCanonicalSections(day);
            validateBlocks(day);
        }
    }

    private void validateBlocks(RoutineDay day) {
        for (RoutineBlock block : day.getBlocks()) {
            if (block.getExercises().isEmpty()) {
                throw new BusinessRuleException("El bloque '" + block.getTitle() + "' (día '" + day.getName() + "') no tiene ejercicios");
            }
            if (block.getStructuralType() == BlockStructuralType.CIRCUIT
                    && (block.getTotalDurationSeconds() == null || block.getTotalDurationSeconds() <= 0)) {
                throw new BusinessRuleException("El bloque '" + block.getTitle() + "' (día '" + day.getName() + "') es CIRCUIT y requiere duración total");
            }
            for (RoutineExercise exercise : block.getExercises()) {
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

    private void validateCanonicalSections(RoutineDay day) {
        boolean hasWarmup = false;
        boolean hasMain = false;
        boolean hasCooldown = false;
        for (RoutineBlock block : day.getBlocks()) {
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
