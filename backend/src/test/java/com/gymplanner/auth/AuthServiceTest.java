package com.gymplanner.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
        when(userRepository.findByEmailIgnoreCase("admin@gymplanner.local")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("admin123", "hash")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(activeUser)).thenReturn("jwt-token");

        LoginResponse response = authService.login(new LoginRequest("admin@gymplanner.local", "admin123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("admin@gymplanner.local");
        assertThat(response.user().gymId()).isEqualTo(1L);
        assertThat(activeUser.getLastLoginAt()).isNotNull();
        verify(userRepository).save(activeUser);
    }

    @Test
    void loginWithWrongPasswordThrowsUnauthorized() {
        when(userRepository.findByEmailIgnoreCase("admin@gymplanner.local")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@gymplanner.local", "bad")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void loginWithInactiveUserThrowsUnauthorized() {
        User inactiveUser = buildUser(false);
        when(userRepository.findByEmailIgnoreCase("admin@gymplanner.local")).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@gymplanner.local", "admin123")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void loginWithMissingEmailThrowsUnauthorized() {
        when(userRepository.findByEmailIgnoreCase("missing@gymplanner.local")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing@gymplanner.local", "admin123")))
                .isInstanceOf(UnauthorizedException.class);
    }

    private User buildUser(boolean active) {
        Gym gym = new Gym();
        gym.setId(1L);
        gym.setName("Demo Gym");

        User user = new User();
        user.setId(1L);
        user.setGym(gym);
        user.setEmail("admin@gymplanner.local");
        user.setPasswordHash("hash");
        user.setFullName("Owner Demo");
        user.setRole(UserRole.OWNER);
        user.setActive(active);
        return user;
    }
}
