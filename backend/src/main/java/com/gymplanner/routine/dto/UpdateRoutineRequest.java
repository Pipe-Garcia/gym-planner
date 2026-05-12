package com.gymplanner.routine.dto;

import com.gymplanner.routine.RoutineStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record UpdateRoutineRequest(@NotBlank @Size(max = 150) String name, @Size(max = 150) String objective, RoutineStatus status, LocalDate assignedDate, LocalDate finishedDate, String generalNotes, String internalNotes, @Valid List<RoutineDayInput> days) {}
