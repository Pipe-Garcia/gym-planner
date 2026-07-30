package com.gymplanner.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

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

    @Test
    void validationError_doesNotReflectRejectedValue() {
        String sentinel = "REJECTED_SENTINEL_6c8e4a";
        String safeMessage = "el tamaño debe ser menor o igual a 100";
        FieldError fieldError = new FieldError(
                "createInjuryRequest",
                "bodyArea",
                sentinel,
                false,
                new String[]{"Size.createInjuryRequest.bodyArea", "Size.bodyArea", "Size"},
                new Object[]{100},
                safeMessage);
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(exception.getBindingResult()).thenReturn(bindingResult);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/students/1/injuries");

        ApiError response = handler.handleValidation(exception, request);

        assertThat(response.fieldErrors().get("bodyArea")).isEqualTo(safeMessage);
        assertThat(response.toString()).doesNotContain(sentinel);
    }
}
