package com.gymplanner.gym;

import com.gymplanner.auth.CustomUserDetailsService.GymPrincipal;
import com.gymplanner.gym.dto.GymResponse;
import com.gymplanner.gym.dto.UpdateGymRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gym")
@RequiredArgsConstructor
public class GymController {

    private final GymService gymService;

    @GetMapping("/current")
    @Operation(summary = "Obtiene el gimnasio actual")
    @ApiResponse(responseCode = "200", description = "Gimnasio actual")
    GymResponse getCurrent(@AuthenticationPrincipal GymPrincipal principal) {
        return gymService.getCurrentGym(principal.gymId());
    }

    @PutMapping("/current")
    @Operation(summary = "Actualiza el gimnasio actual")
    @ApiResponse(responseCode = "200", description = "Gimnasio actualizado")
    GymResponse updateCurrent(
            @AuthenticationPrincipal GymPrincipal principal,
            @Valid @RequestBody UpdateGymRequest request) {
        return gymService.updateCurrentGym(principal.gymId(), request);
    }
}
