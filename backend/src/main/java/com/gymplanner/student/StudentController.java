package com.gymplanner.student;

import com.gymplanner.auth.CustomUserDetailsService.GymPrincipal;
import com.gymplanner.shared.pagination.PageResponse;
import com.gymplanner.student.dto.CreateStudentRequest;
import com.gymplanner.student.dto.PhoneCheckResponse;
import com.gymplanner.student.dto.StudentResponse;
import com.gymplanner.student.dto.StudentSummaryResponse;
import com.gymplanner.student.dto.UpdateStudentRequest;
import jakarta.validation.Valid;
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
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    PageResponse<StudentSummaryResponse> list(
            @AuthenticationPrincipal GymPrincipal principal,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "true") Boolean active,
            @RequestParam(required = false) String sport,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName,asc") String sort) {
        return studentService.list(principal.gymId(), search, active, sport, level, pageable(page, size, sort));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    StudentResponse create(
            @AuthenticationPrincipal GymPrincipal principal,
            @Valid @RequestBody CreateStudentRequest request) {
        return studentService.create(principal.gymId(), request);
    }

    @GetMapping("/check-phone")
    PhoneCheckResponse checkPhone(
            @AuthenticationPrincipal GymPrincipal principal,
            @RequestParam String phone,
            @RequestParam(required = false) Long excludeId) {
        return studentService.checkPhone(principal.gymId(), phone, excludeId);
    }

    @GetMapping("/{id}")
    StudentResponse get(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id) {
        return studentService.get(principal.gymId(), id);
    }

    @PutMapping("/{id}")
    StudentResponse update(
            @AuthenticationPrincipal GymPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateStudentRequest request) {
        return studentService.update(principal.gymId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id) {
        studentService.deactivate(principal.gymId(), id);
    }

    @PatchMapping("/{id}/reactivate")
    StudentResponse reactivate(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id) {
        return studentService.reactivate(principal.gymId(), id);
    }

    private Pageable pageable(int page, int size, String sort) {
        String[] parts = sort.split(",", 2);
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, parts[0]));
    }
}
