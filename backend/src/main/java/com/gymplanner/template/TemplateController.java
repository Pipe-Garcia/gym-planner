package com.gymplanner.template;

import com.gymplanner.auth.CustomUserDetailsService.GymPrincipal;
import com.gymplanner.shared.pagination.PageResponse;
import com.gymplanner.template.dto.CreateTemplateRequest;
import com.gymplanner.template.dto.TemplateResponse;
import com.gymplanner.template.dto.TemplateSummaryResponse;
import com.gymplanner.template.dto.UpdateTemplateRequest;
import jakarta.validation.Valid;
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
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {
    private final TemplateService templateService;

    @GetMapping
    PageResponse<TemplateSummaryResponse> list(@AuthenticationPrincipal GymPrincipal principal, @RequestParam(required = false) String search, @RequestParam(required = false) String sport, @RequestParam(required = false) String objective, @RequestParam(required = false) String level, @RequestParam(defaultValue = "true") Boolean active, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "name,asc") String sort) {
        return templateService.list(principal.gymId(), search, sport, objective, level, active, pageable(page, size, sort));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TemplateResponse create(@AuthenticationPrincipal GymPrincipal principal, @Valid @RequestBody CreateTemplateRequest request) {
        return templateService.create(principal.gymId(), principal.id(), request);
    }

    @GetMapping("/{id}")
    TemplateResponse get(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id) {
        return templateService.get(principal.gymId(), id);
    }

    @PutMapping("/{id}")
    TemplateResponse update(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id, @Valid @RequestBody UpdateTemplateRequest request) {
        return templateService.update(principal.gymId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id) {
        templateService.deactivate(principal.gymId(), id);
    }

    @PostMapping("/{id}/reactivate")
    TemplateResponse reactivate(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id) {
        return templateService.reactivate(principal.gymId(), id);
    }

    @PostMapping("/{id}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    TemplateResponse duplicate(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id) {
        return templateService.duplicate(principal.gymId(), principal.id(), id);
    }

    private Pageable pageable(int page, int size, String sort) {
        String[] parts = sort.split(",", 2);
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, parts[0]));
    }
}
