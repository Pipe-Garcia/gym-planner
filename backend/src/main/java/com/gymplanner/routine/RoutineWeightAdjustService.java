package com.gymplanner.routine;

import com.gymplanner.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class RoutineWeightAdjustService {

    public int applyAdjustment(Routine routine, RoutineWeightAdjustmentScopeType scopeType, double percentage, Double roundingStepKg) {
        if (scopeType != RoutineWeightAdjustmentScopeType.ROUTINE) {
            throw new BusinessRuleException("Scope de ajuste de pesos no soportado.");
        }
        BigDecimal multiplier = BigDecimal.ONE.add(BigDecimal.valueOf(percentage).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        BigDecimal step = roundingStepKg == null ? null : BigDecimal.valueOf(roundingStepKg);
        int adjusted = 0;
        for (RoutineDay day : routine.getDays()) {
            for (RoutineBlock block : day.getBlocks()) {
                for (RoutineExercise exercise : block.getExercises()) {
                    for (RoutineExerciseSet set : exercise.getSets()) {
                        if (set.getTargetWeightKg() != null) {
                            BigDecimal adjustedWeight = set.getTargetWeightKg().multiply(multiplier);
                            set.setTargetWeightKg(round(adjustedWeight, step));
                            adjusted++;
                        }
                    }
                }
            }
        }
        return adjusted;
    }

    private BigDecimal round(BigDecimal value, BigDecimal step) {
        if (step == null) {
            return value.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal steps = value.divide(step, 0, RoundingMode.HALF_UP);
        return steps.multiply(step).setScale(2, RoundingMode.HALF_UP);
    }
}
