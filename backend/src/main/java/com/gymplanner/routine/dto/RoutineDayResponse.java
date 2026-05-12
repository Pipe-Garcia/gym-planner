package com.gymplanner.routine.dto;

import java.util.List;

public record RoutineDayResponse(Long id, int orderIndex, String name, String notes, List<RoutineBlockResponse> blocks) {}
