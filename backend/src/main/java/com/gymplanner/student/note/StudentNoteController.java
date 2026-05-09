package com.gymplanner.student.note;

import com.gymplanner.auth.CustomUserDetailsService.GymPrincipal;
import com.gymplanner.student.note.dto.CreateNoteRequest;
import com.gymplanner.student.note.dto.NoteResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students/{studentId}/notes")
@RequiredArgsConstructor
public class StudentNoteController {

    private final StudentNoteService noteService;

    @GetMapping
    List<NoteResponse> list(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long studentId) {
        return noteService.list(principal.gymId(), studentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    NoteResponse create(
            @AuthenticationPrincipal GymPrincipal principal,
            @PathVariable Long studentId,
            @Valid @RequestBody CreateNoteRequest request) {
        return noteService.create(principal.gymId(), studentId, principal.id(), request);
    }

    @DeleteMapping("/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @AuthenticationPrincipal GymPrincipal principal,
            @PathVariable Long studentId,
            @PathVariable Long noteId) {
        noteService.delete(principal.gymId(), studentId, noteId, principal.id(), principal.role());
    }
}
