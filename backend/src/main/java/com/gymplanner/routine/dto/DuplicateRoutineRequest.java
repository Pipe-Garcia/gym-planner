package com.gymplanner.routine.dto;

import com.gymplanner.routine.RoutineStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record DuplicateRoutineRequest(@NotNull Long targetStudentId, @Size(max = 150) String name, LocalDate assignedDate, RoutineStatus status) {}
