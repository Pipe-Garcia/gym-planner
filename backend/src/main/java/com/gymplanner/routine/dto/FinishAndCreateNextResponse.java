package com.gymplanner.routine.dto;

public record FinishAndCreateNextResponse(
        RoutineSummaryResponse finishedRoutine,
        RoutineResponse newRoutine,
        int weightSetsAdjusted
) {}
