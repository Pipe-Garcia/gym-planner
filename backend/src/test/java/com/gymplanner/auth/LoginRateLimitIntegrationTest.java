package com.gymplanner.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymplanner.auth.dto.LoginRequest;
import com.gymplanner.user.User;
import com.gymplanner.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoginRateLimitIntegrationTest {

    private static final String TEST_EMAIL = "owner@test.local";
    private static final String TEST_PASSWORD = "integration-test-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpOwnerCredentials() {
        User owner = userRepository.findByEmailIgnoreCase(TEST_EMAIL).orElseThrow();
        owner.setActive(true);
        owner.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        userRepository.saveAndFlush(owner);
    }

    @Test
    void sixthLoginAttemptFromSameIpIsRateLimited() throws Exception {
        String clientIp = "203.0.113.11";

        for (int attempt = 0; attempt < LoginRateLimiter.CAPACITY; attempt++) {
            performLogin(clientIp, TEST_EMAIL, TEST_PASSWORD)
                    .andExpect(status().isOk());
        }

        performLogin(clientIp, TEST_EMAIL, TEST_PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, matchesPattern("[1-9][0-9]*")))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message")
                        .value("Too many login attempts. Please try again later."));
    }

    @Test
    void exhaustedIpDoesNotAffectDifferentIp() throws Exception {
        String exhaustedIp = "203.0.113.12";
        String differentIp = "198.51.100.12";

        for (int attempt = 0; attempt < LoginRateLimiter.CAPACITY; attempt++) {
            performLogin(exhaustedIp, TEST_EMAIL, TEST_PASSWORD)
                    .andExpect(status().isOk());
        }
        performLogin(exhaustedIp, TEST_EMAIL, TEST_PASSWORD)
                .andExpect(status().isTooManyRequests());

        performLogin(differentIp, TEST_EMAIL, TEST_PASSWORD)
                .andExpect(status().isOk());
    }

    @Test
    void normalSuccessfulLoginIsAllowed() throws Exception {
        performLogin("203.0.113.13", TEST_EMAIL, TEST_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void nonLoginEndpointIsNotRateLimited() throws Exception {
        for (int attempt = 0; attempt < LoginRateLimiter.CAPACITY + 1; attempt++) {
            mockMvc.perform(get("/api/public/ping")
                            .with(request -> {
                                request.setRemoteAddr("203.0.113.14");
                                return request;
                            }))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void missingEmailAndWrongPasswordHaveIdenticalHttpErrors() throws Exception {
        MvcResult wrongPassword = performLogin("203.0.113.15", TEST_EMAIL, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andReturn();
        MvcResult missingEmail = performLogin(
                        "198.51.100.15",
                        "missing-user@test.local",
                        "wrong-password")
                .andExpect(status().isUnauthorized())
                .andReturn();

        JsonNode wrongPasswordBody =
                objectMapper.readTree(wrongPassword.getResponse().getContentAsByteArray());
        JsonNode missingEmailBody =
                objectMapper.readTree(missingEmail.getResponse().getContentAsByteArray());

        assertThat(missingEmailBody.path("status").asInt())
                .isEqualTo(wrongPasswordBody.path("status").asInt())
                .isEqualTo(401);
        assertThat(missingEmailBody.path("message").asText())
                .isEqualTo(wrongPasswordBody.path("message").asText())
                .isEqualTo("Invalid email or password.");
    }

    private org.springframework.test.web.servlet.ResultActions performLogin(
            String clientIp,
            String email,
            String password) throws Exception {
        return mockMvc.perform(post(LoginRateLimitFilter.LOGIN_PATH)
                .with(request -> {
                    // Local/test deliberately uses the servlet remote address: no trusted proxy exists here.
                    request.setRemoteAddr(clientIp);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new LoginRequest(email, password))));
    }
}
