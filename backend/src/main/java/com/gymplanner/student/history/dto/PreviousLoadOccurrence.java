package com.gymplanner.student.history.dto;

import com.gymplanner.exercise.MeasurementType;
import com.gymplanner.routine.RoutineStatus;
import com.gymplanner.shared.blocks.BlockPurpose;
import com.gymplanner.shared.blocks.BlockStructuralType;
import java.time.LocalDate;
import java.util.List;

public record PreviousLoadOccurrence(
        Long routineId,
        String routineName,
        RoutineStatus routineStatus,
        LocalDate assignedDate,
        LocalDate finishedDate,
        int dayOrderIndex,
        String dayName,
        String blockTitle,
        BlockStructuralType blockStructuralType,
        BlockPurpose blockPurpose,
        String exerciseNotes,
        MeasurementType measurementType,
        List<PreviousLoadSet> sets) {
}
