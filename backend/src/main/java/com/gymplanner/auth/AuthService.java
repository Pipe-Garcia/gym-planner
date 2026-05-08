package com.gymplanner.auth;

import com.gymplanner.auth.dto.AuthenticatedUserResponse;
import com.gymplanner.auth.dto.LoginRequest;
import com.gymplanner.auth.dto.LoginResponse;
import com.gymplanner.shared.exception.UnauthorizedException;
import com.gymplanner.user.User;
import com.gymplanner.user.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!user.isActive()) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        user.setLastLoginAt(Instant.now());
        User saved = userRepository.save(user);
        return new LoginResponse(jwtService.generateToken(saved), toAuthenticatedUser(saved));
    }

    public AuthenticatedUserResponse toAuthenticatedUser(User user) {
        return new AuthenticatedUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getGym().getId());
    }
}
