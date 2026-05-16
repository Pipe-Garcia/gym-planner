package com.gymplanner.routine.dto;

public record CreateNextRoutineResponse(
        RoutineSummaryResponse sourceRoutine,
        RoutineResponse newRoutine,
        int weightSetsAdjusted
) {}
