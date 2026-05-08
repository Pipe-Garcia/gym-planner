package com.gymplanner.auth;

import com.gymplanner.auth.CustomUserDetailsService.GymPrincipal;
import com.gymplanner.auth.dto.AuthenticatedUserResponse;
import com.gymplanner.auth.dto.LoginRequest;
import com.gymplanner.auth.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/api/auth/login")
    @Operation(summary = "Login con email y password")
    @ApiResponse(responseCode = "200", description = "Login exitoso")
    @ApiResponse(responseCode = "401", description = "Credenciales invalidas")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/api/me")
    @Operation(summary = "Obtiene el usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Usuario autenticado")
    AuthenticatedUserResponse me(@AuthenticationPrincipal GymPrincipal principal) {
        return new AuthenticatedUserResponse(
                principal.id(),
                principal.email(),
                principal.fullName(),
                principal.role(),
                principal.gymId());
    }
}
