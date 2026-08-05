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

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password.";
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$12$lr2GVLfVk4BgcjlRzEh0zu9E1TP7N7XKuYE1DpYZ8cPqEdtqIip5a";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseGet(() -> {
                    passwordEncoder.matches(request.password(), DUMMY_PASSWORD_HASH);
                    throw new UnauthorizedException(INVALID_CREDENTIALS_MESSAGE);
                });

        if (!user.isActive()) {
            throw new UnauthorizedException(INVALID_CREDENTIALS_MESSAGE);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException(INVALID_CREDENTIALS_MESSAGE);
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
