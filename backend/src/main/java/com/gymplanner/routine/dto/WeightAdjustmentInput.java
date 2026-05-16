package com.gymplanner.routine.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record WeightAdjustmentInput(
        @NotNull
        @DecimalMin(value = "-90", inclusive = false)
        @DecimalMax(value = "900", inclusive = false)
        Double percentage,
        Double roundingStepKg
) {
    private static final Set<Double> ALLOWED_ROUNDING_STEPS = Set.of(0.5, 1.0, 2.5, 5.0);

    @AssertTrue(message = "roundingStepKg debe ser 0.5, 1.0, 2.5 o 5.0")
    public boolean isRoundingStepKgAllowed() {
        return roundingStepKg == null || ALLOWED_ROUNDING_STEPS.contains(roundingStepKg);
    }
}
