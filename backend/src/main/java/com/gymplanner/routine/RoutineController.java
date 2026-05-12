package com.gymplanner.routine;

import com.gymplanner.auth.CustomUserDetailsService.GymPrincipal;
import com.gymplanner.routine.dto.CreateRoutineFromScratchRequest;
import com.gymplanner.routine.dto.CreateRoutineFromTemplateRequest;
import com.gymplanner.routine.dto.DuplicateRoutineRequest;
import com.gymplanner.routine.dto.RoutineResponse;
import com.gymplanner.routine.dto.RoutineSummaryResponse;
import com.gymplanner.routine.dto.UpdateRoutineRequest;
import com.gymplanner.shared.pagination.PageResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoutineController {
    private final RoutineService routineService;
    private final RoutineFromTemplateService routineFromTemplateService;

    @GetMapping("/api/students/{studentId}/routines")
    PageResponse<RoutineSummaryResponse> listForStudent(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long studentId, @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "assignedDate,desc") String sort) {
        return routineService.listForStudent(principal.gymId(), studentId, status, pageable(page, size, sort));
    }

    @GetMapping("/api/routines")
    PageResponse<RoutineSummaryResponse> list(@AuthenticationPrincipal GymPrincipal principal, @RequestParam(required = false) String status, @RequestParam(required = false) String q, @RequestParam(required = false) LocalDate dateFrom, @RequestParam(required = false) LocalDate dateTo, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "assignedDate,desc") String sort) {
        return routineService.list(principal.gymId(), status, q, dateFrom, dateTo, pageable(page, size, sort));
    }

    @GetMapping("/api/students/{studentId}/routines/active")
    RoutineResponse getActive(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long studentId) {
        return routineService.getActive(principal.gymId(), studentId);
    }

    @PostMapping("/api/routines/from-scratch")
    @ResponseStatus(HttpStatus.CREATED)
    RoutineResponse createFromScratch(@AuthenticationPrincipal GymPrincipal principal, @Valid @RequestBody CreateRoutineFromScratchRequest request) {
        return routineService.createFromScratch(principal.gymId(), principal.id(), request);
    }

    @PostMapping("/api/routines/from-template")
    @ResponseStatus(HttpStatus.CREATED)
    RoutineResponse createFromTemplate(@AuthenticationPrincipal GymPrincipal principal, @Valid @RequestBody CreateRoutineFromTemplateRequest request) {
        return routineFromTemplateService.createFromTemplate(principal.gymId(), principal.id(), request);
    }

    @GetMapping("/api/routines/{id}")
    RoutineResponse get(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id) {
        return routineService.get(principal.gymId(), id);
    }

    @PostMapping("/api/routines/{id}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    RoutineResponse duplicate(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id, @Valid @RequestBody DuplicateRoutineRequest request) {
        return routineService.duplicate(principal.gymId(), principal.id(), id, request);
    }

    @PutMapping("/api/routines/{id}")
    RoutineResponse update(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id, @Valid @RequestBody UpdateRoutineRequest request) {
        return routineService.update(principal.gymId(), id, request);
    }

    @PostMapping("/api/routines/{id}/finish")
    RoutineResponse finish(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id) {
        return routineService.finish(principal.gymId(), id);
    }

    @PostMapping("/api/routines/{id}/archive")
    RoutineResponse archive(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id) {
        return routineService.archive(principal.gymId(), id);
    }

    @PostMapping("/api/routines/{id}/activate")
    RoutineResponse activate(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id) {
        return routineService.activate(principal.gymId(), id);
    }

    @DeleteMapping("/api/routines/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id) {
        routineService.delete(principal.gymId(), id);
    }

    private Pageable pageable(int page, int size, String sort) {
        String[] parts = sort.split(",", 2);
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, parts[0]));
    }
}
