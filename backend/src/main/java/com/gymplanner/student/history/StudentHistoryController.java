package com.gymplanner.student.history;

import com.gymplanner.auth.CustomUserDetailsService.GymPrincipal;
import com.gymplanner.shared.pagination.PageResponse;
import com.gymplanner.student.history.dto.StudentExerciseHistoryItemResponse;
import com.gymplanner.student.history.dto.StudentExerciseOccurrenceResponse;
import com.gymplanner.student.history.dto.StudentHistorySummaryResponse;
import com.gymplanner.student.history.dto.StudentRoutineTimelineItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StudentHistoryController {
    private final StudentHistoryService studentHistoryService;

    @GetMapping("/api/students/{studentId}/history/summary")
    @Operation(summary = "Resumen historico del alumno")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen historico"),
            @ApiResponse(responseCode = "401", description = "Autenticacion requerida"),
            @ApiResponse(responseCode = "404", description = "Alumno no encontrado")
    })
    StudentHistorySummaryResponse summary(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long studentId) {
        return studentHistoryService.getSummary(principal.gymId(), studentId);
    }

    @GetMapping("/api/students/{studentId}/history/timeline")
    @Operation(summary = "Timeline de ciclos del alumno")
    PageResponse<StudentRoutineTimelineItemResponse> timeline(
            @AuthenticationPrincipal GymPrincipal principal,
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return studentHistoryService.getTimeline(principal.gymId(), studentId, page, size);
    }

    @GetMapping("/api/students/{studentId}/history/exercises")
    @Operation(summary = "Ejercicios trabajados historicamente por el alumno")
    PageResponse<StudentExerciseHistoryItemResponse> exercises(
            @AuthenticationPrincipal GymPrincipal principal,
            @PathVariable Long studentId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return studentHistoryService.getExerciseHistory(principal.gymId(), studentId, search, page, size);
    }

    @GetMapping("/api/students/{studentId}/history/exercises/{exerciseId}/occurrences")
    @Operation(summary = "Ocurrencias historicas de un ejercicio del alumno")
    PageResponse<StudentExerciseOccurrenceResponse> occurrences(
            @AuthenticationPrincipal GymPrincipal principal,
            @PathVariable Long studentId,
            @PathVariable Long exerciseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return studentHistoryService.getExerciseOccurrences(principal.gymId(), studentId, exerciseId, page, size);
    }
}
