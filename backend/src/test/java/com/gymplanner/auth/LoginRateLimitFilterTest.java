package com.gymplanner.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LoginRateLimitFilterTest {

    @Test
    void internalRateLimiterFailureAllowsLoginRequestToContinue() throws Exception {
        ClientIpExtractor clientIpExtractor = mock(ClientIpExtractor.class);
        LoginRateLimiter loginRateLimiter = mock(LoginRateLimiter.class);
        LoginRateLimitFilter filter =
                new LoginRateLimitFilter(clientIpExtractor, loginRateLimiter, new ObjectMapper());
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
}
