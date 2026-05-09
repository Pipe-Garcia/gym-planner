package com.gymplanner.exercise.tag;

import com.gymplanner.auth.CustomUserDetailsService.GymPrincipal;
import com.gymplanner.exercise.tag.dto.TagResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercise-tags")
@RequiredArgsConstructor
public class ExerciseTagController {

    private final ExerciseTagService tagService;

    @GetMapping
    List<TagResponse> list(@AuthenticationPrincipal GymPrincipal principal, @RequestParam(required = false) TagType type) {
        return tagService.list(principal.gymId(), type);
    }
}
