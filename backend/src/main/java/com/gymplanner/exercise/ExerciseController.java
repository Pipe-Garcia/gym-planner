package com.gymplanner.exercise;

import com.gymplanner.auth.CustomUserDetailsService.GymPrincipal;
import com.gymplanner.exercise.dto.CreateExerciseRequest;
import com.gymplanner.exercise.dto.ExerciseResponse;
import com.gymplanner.exercise.dto.ExerciseSummaryResponse;
import com.gymplanner.exercise.dto.UpdateExerciseRequest;
import com.gymplanner.shared.pagination.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping
    PageResponse<ExerciseSummaryResponse> list(
            @AuthenticationPrincipal GymPrincipal principal,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(defaultValue = "true") Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort) {
        return exerciseService.list(principal.gymId(), search, tagIds, active, pageable(page, size, sort));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ExerciseResponse create(
            @AuthenticationPrincipal GymPrincipal principal,
            @Valid @RequestBody CreateExerciseRequest request) {
        return exerciseService.create(principal.gymId(), request);
    }

    @GetMapping("/{id}")
    ExerciseResponse get(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id) {
        return exerciseService.get(principal.gymId(), id);
    }

    @PutMapping("/{id}")
    ExerciseResponse update(
            @AuthenticationPrincipal GymPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateExerciseRequest request) {
        return exerciseService.update(principal.gymId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id) {
        exerciseService.deactivate(principal.gymId(), id);
    }

    @PatchMapping("/{id}/reactivate")
    ExerciseResponse reactivate(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id) {
        return exerciseService.reactivate(principal.gymId(), id);
    }

    private Pageable pageable(int page, int size, String sort) {
        String[] parts = sort.split(",", 2);
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, parts[0]));
    }
}
