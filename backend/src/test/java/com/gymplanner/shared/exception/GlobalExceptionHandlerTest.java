package com.gymplanner.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void dataIntegrityViolationReturnsGenericConflict() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/students");

        ApiError response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("constraint violation"),
                request);

        assertThat(response.status()).isEqualTo(409);
        assertThat(response.error()).isEqualTo("Conflict");
        assertThat(response.message()).isEqualTo("Ya existe un alumno con esos datos.");
        assertThat(response.path()).isEqualTo("/api/students");
        assertThat(response.fieldErrors()).isEmpty();
    }
}
