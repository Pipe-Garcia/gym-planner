package com.gymplanner.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymplanner.user.User;
import com.gymplanner.user.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JwtAuthenticationFilterIntegrationTest {

    private static final String JWT_SECRET = "test-secret-with-at-least-32-characters";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void validTokenForActiveUserReturnsOk() throws Exception {
        User user = createUser(true);
        String token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()));
    }

    @Test
    void validTokenForInactiveUserReturnsUnauthorized() throws Exception {
        User user = createUser(false);
        String token = jwtService.generateToken(user);

        assertUnauthorized(token);
    }

    @Test
    void validTokenForDeletedUserReturnsUnauthorized() throws Exception {
        User user = createUser(true);
        String token = jwtService.generateToken(user);
        userRepository.delete(user);
        userRepository.flush();
        entityManager.clear();

        assertUnauthorized(token);
    }

    @Test
    void expiredTokenReturnsUnauthorized() throws Exception {
        User user = createUser(true);
        JwtService expiredTokenService = new JwtService(JWT_SECRET, Duration.ofSeconds(-1));
        String token = expiredTokenService.generateToken(user);

        assertUnauthorized(token);
    }

    @Test
    void malformedTokenReturnsUnauthorized() throws Exception {
        assertUnauthorized("not-a-jwt");
    }

    @Test
    void tokenWithInvalidSignatureReturnsUnauthorized() throws Exception {
        User user = createUser(true);
        JwtService otherSigner = new JwtService(
                "different-test-secret-with-at-least-32-characters",
                Duration.ofHours(12));
        String token = otherSigner.generateToken(user);

        assertUnauthorized(token);
    }

    private void assertUnauthorized(String token) throws Exception {
        mockMvc.perform(get("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    private User createUser(boolean active) {
        Long id = ((Number) entityManager.createNativeQuery("SELECT COALESCE(MAX(id), 0) + 1 FROM users")
                .getSingleResult()).longValue();
        String email = "jwt-filter-" + UUID.randomUUID() + "@test.local";
        entityManager.createNativeQuery("""
                        INSERT INTO users (
                            id, gym_id, email, password_hash, full_name, role, active, created_at, updated_at
                        )
                        VALUES (
                            :id, 1, :email, 'hash', 'JWT Filter User', 'OWNER', :active,
                            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                        )
                        """)
                .setParameter("id", id)
                .setParameter("email", email)
                .setParameter("active", active)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        return userRepository.findByEmailIgnoreCase(email).orElseThrow();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
