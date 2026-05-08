package com.gymplanner.auth;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymplanner.auth.dto.AuthenticatedUserResponse;
import com.gymplanner.auth.dto.LoginRequest;
import com.gymplanner.auth.dto.LoginResponse;
import com.gymplanner.shared.exception.UnauthorizedException;
import com.gymplanner.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void loginWithValidCredentialsReturnsTokenAndUser() throws Exception {
        LoginRequest request = new LoginRequest("admin@gymplanner.local", "admin123");
        when(authService.login(request)).thenReturn(new LoginResponse(
                "jwt-token",
                new AuthenticatedUserResponse(1L, "admin@gymplanner.local", "Owner Demo", UserRole.OWNER, 1L)));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@gymplanner.local","password":"admin123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value("admin@gymplanner.local"))
                .andExpect(jsonPath("$.user.gymId").value(1));
    }

    @Test
    void loginWithInvalidCredentialsReturnsUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest("admin@gymplanner.local", "bad");
        when(authService.login(request)).thenThrow(new UnauthorizedException("Invalid email or password."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@gymplanner.local","password":"bad"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void loginWithInvalidEmailReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"invalid","password":"admin123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }
}
