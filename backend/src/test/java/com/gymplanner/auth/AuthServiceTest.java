package com.gymplanner.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymplanner.auth.dto.LoginRequest;
import com.gymplanner.auth.dto.LoginResponse;
import com.gymplanner.gym.Gym;
import com.gymplanner.shared.exception.UnauthorizedException;
import com.gymplanner.user.User;
import com.gymplanner.user.UserRepository;
import com.gymplanner.user.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;
    private User activeUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
        activeUser = buildUser(true);
    }

    @Test
    void loginWithValidCredentialsReturnsToken() {
        when(userRepository.findByEmailIgnoreCase("owner@test.local")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("test-password", "hash")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(activeUser)).thenReturn("jwt-token");

        LoginResponse response = authService.login(new LoginRequest("owner@test.local", "test-password"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("owner@test.local");
        assertThat(response.user().gymId()).isEqualTo(1L);
        assertThat(activeUser.getLastLoginAt()).isNotNull();
        verify(userRepository).save(activeUser);
    }

    @Test
    void loginWithWrongPasswordThrowsUnauthorized() {
        when(userRepository.findByEmailIgnoreCase("owner@test.local")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("owner@test.local", "bad")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void loginWithInactiveUserThrowsUnauthorized() {
        User inactiveUser = buildUser(false);
        when(userRepository.findByEmailIgnoreCase("owner@test.local")).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> authService.login(new LoginRequest("owner@test.local", "test-password")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void loginWithMissingEmailThrowsUnauthorized() {
        when(userRepository.findByEmailIgnoreCase("missing@gymplanner.local")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing@gymplanner.local", "test-password")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password.");

        verify(passwordEncoder).matches(
                eq("test-password"),
                argThat(hash -> hash != null && hash.matches("\\$2[aby]\\$12\\$.{53}")));
    }

    private User buildUser(boolean active) {
        Gym gym = new Gym();
        gym.setId(1L);
        gym.setName("Demo Gym");

        User user = new User();
        user.setId(1L);
        user.setGym(gym);
        user.setEmail("owner@test.local");
        user.setPasswordHash("hash");
        user.setFullName("Test Owner");
        user.setRole(UserRole.OWNER);
        user.setActive(active);
        return user;
    }
}
