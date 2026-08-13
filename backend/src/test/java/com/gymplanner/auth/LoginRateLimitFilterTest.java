package com.gymplanner.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LoginRateLimitFilterTest {

    @Test
    void internalRateLimiterFailureAllowsLoginRequestToContinue() throws Exception {
        ClientIpExtractor clientIpExtractor = mock(ClientIpExtractor.class);
        LoginRateLimiter loginRateLimiter = mock(LoginRateLimiter.class);
        LoginRateLimitDiagnostics diagnostics = new LoginRateLimitDiagnostics(false);
        LoginRateLimitFilter filter =
                new LoginRateLimitFilter(
                        clientIpExtractor,
                        loginRateLimiter,
                        diagnostics,
                        new ObjectMapper());
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", LoginRateLimitFilter.LOGIN_PATH);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(clientIpExtractor.extract(request)).thenReturn("203.0.113.90");
        when(loginRateLimiter.tryAcquire("203.0.113.90"))
                .thenThrow(new IllegalStateException("simulated limiter failure"));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void enabledDiagnosticsLogOnlyMetadataAndFingerprint() throws Exception {
        ClientIpExtractor clientIpExtractor = new ClientIpExtractor("cloudflare");
        LoginRateLimiter loginRateLimiter = mock(LoginRateLimiter.class);
        LoginRateLimitDiagnostics diagnostics = new LoginRateLimitDiagnostics(true);
        LoginRateLimitFilter filter = new LoginRateLimitFilter(
                clientIpExtractor,
                loginRateLimiter,
                diagnostics,
                new ObjectMapper().findAndRegisterModules());
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", LoginRateLimitFilter.LOGIN_PATH);
        request.addHeader("CF-Connecting-IP", "203.0.113.91");
        request.addHeader("X-Forwarded-For", "198.51.100.91");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(loginRateLimiter.tryAcquire("203.0.113.91"))
                .thenReturn(LoginRateLimiter.Decision.deny(37));

        Logger logger = (Logger) LoggerFactory.getLogger(LoginRateLimitFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext(logger.getLoggerContext());
        appender.start();
        logger.addAppender(appender);
        try {
            filter.doFilter(request, response, filterChain);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        List<String> diagnosticMessages = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.startsWith("LOGIN_RL_DIAG"))
                .toList();
        assertThat(diagnosticMessages).singleElement().satisfies(message -> {
            assertThat(message)
                    .contains("source=cloudflare")
                    .contains("cfPresent=true")
                    .contains("cfValid=true")
                    .contains("xffPresent=true")
                    .contains("unknownClient=false")
                    .containsPattern("keyHash=[0-9a-f]{12}")
                    .contains("allowed=false")
                    .contains("retryAfterSeconds=37")
                    .doesNotContain("203.0.113.91")
                    .doesNotContain("198.51.100.91");
        });
        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void disabledDiagnosticsDoNotLog() throws Exception {
        ClientIpExtractor clientIpExtractor = new ClientIpExtractor("remote-address");
        LoginRateLimiter loginRateLimiter = mock(LoginRateLimiter.class);
        LoginRateLimitFilter filter = new LoginRateLimitFilter(
                clientIpExtractor,
                loginRateLimiter,
                new LoginRateLimitDiagnostics(false),
                new ObjectMapper());
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", LoginRateLimitFilter.LOGIN_PATH);
        request.setRemoteAddr("203.0.113.92");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(loginRateLimiter.tryAcquire("203.0.113.92"))
                .thenReturn(LoginRateLimiter.Decision.permit());

        Logger logger = (Logger) LoggerFactory.getLogger(LoginRateLimitFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext(logger.getLoggerContext());
        appender.start();
        logger.addAppender(appender);
        try {
            filter.doFilter(request, response, filterChain);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(message -> message.startsWith("LOGIN_RL_DIAG"));
        verify(filterChain).doFilter(request, response);
    }
}
