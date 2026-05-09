package com.gymplanner.student.injury;

import com.gymplanner.auth.CustomUserDetailsService.GymPrincipal;
import com.gymplanner.student.injury.dto.CreateInjuryRequest;
import com.gymplanner.student.injury.dto.InjuryResponse;
import com.gymplanner.student.injury.dto.UpdateInjuryRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/students/{studentId}/injuries")
@RequiredArgsConstructor
public class StudentInjuryController {

    private final StudentInjuryService injuryService;

    @GetMapping
    List<InjuryResponse> list(
            @AuthenticationPrincipal GymPrincipal principal,
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "true") Boolean active) {
        return injuryService.list(principal.gymId(), studentId, active);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    InjuryResponse create(
            @AuthenticationPrincipal GymPrincipal principal,
            @PathVariable Long studentId,
            @Valid @RequestBody CreateInjuryRequest request) {
        return injuryService.create(principal.gymId(), studentId, request);
    }

    @PutMapping("/{injuryId}")
    InjuryResponse update(
            @AuthenticationPrincipal GymPrincipal principal,
            @PathVariable Long studentId,
            @PathVariable Long injuryId,
            @Valid @RequestBody UpdateInjuryRequest request) {
        return injuryService.update(principal.gymId(), studentId, injuryId, request);
    }

    @DeleteMapping("/{injuryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(
            @AuthenticationPrincipal GymPrincipal principal,
            @PathVariable Long studentId,
            @PathVariable Long injuryId) {
        injuryService.deactivate(principal.gymId(), studentId, injuryId);
    }
}
