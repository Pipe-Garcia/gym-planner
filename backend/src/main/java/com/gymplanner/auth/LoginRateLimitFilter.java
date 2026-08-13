package com.gymplanner.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymplanner.shared.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

    static final String LOGIN_PATH = "/api/auth/login";
    private static final String RATE_LIMIT_MESSAGE = "Too many login attempts. Please try again later.";

    private final ClientIpExtractor clientIpExtractor;
    private final LoginRateLimiter loginRateLimiter;
    private final LoginRateLimitDiagnostics diagnostics;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            String clientKey = clientIpExtractor.extract(request);
            LoginRateLimiter.Decision decision = loginRateLimiter.tryAcquire(clientKey);
            logDiagnostics(request, clientKey, decision);
            if (!decision.allowed()) {
                writeRateLimitError(request, response, decision.retryAfterSeconds());
                return;
            }
        } catch (RuntimeException exception) {
            log.warn("Login rate limiter failed open due to {}.", exception.getClass().getSimpleName());
        }

        filterChain.doFilter(request, response);
    }

    private void logDiagnostics(
            HttpServletRequest request,
            String clientKey,
            LoginRateLimiter.Decision decision) {
        if (!diagnostics.isEnabled()) {
            return;
        }

        try {
            ClientIpExtractor.DiagnosticContext context =
                    clientIpExtractor.diagnosticContext(request, clientKey);
            log.info(
                    "LOGIN_RL_DIAG source={} cfPresent={} cfValid={} xffPresent={} unknownClient={} keyHash={} allowed={} retryAfterSeconds={}",
                    context.source(),
                    context.cfPresent(),
                    context.cfValid(),
                    context.xffPresent(),
                    context.unknownClient(),
                    diagnostics.fingerprint(clientKey),
                    decision.allowed(),
                    decision.retryAfterSeconds());
        } catch (RuntimeException exception) {
            // Temporary diagnostics must never change the rate-limit decision.
            log.warn("LOGIN_RL_DIAG unavailable reason={}", exception.getClass().getSimpleName());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        /*
         * Request URI is populated consistently by MockMvc and servlet containers.
         * Removing the context path keeps the match stable if the app is mounted
         * below "/" and does not depend on servlet-mapping details.
         */
        String applicationPath = request.getRequestURI().substring(request.getContextPath().length());
        return !HttpMethod.POST.matches(request.getMethod())
                || !LOGIN_PATH.equals(applicationPath);
    }

    private void writeRateLimitError(
            HttpServletRequest request,
            HttpServletResponse response,
            long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        ApiError apiError = new ApiError(
                Instant.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too Many Requests",
                RATE_LIMIT_MESSAGE,
                request.getRequestURI(),
                Map.of());
        objectMapper.writeValue(response.getOutputStream(), apiError);
    }
}
