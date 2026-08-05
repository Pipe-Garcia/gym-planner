package com.gymplanner.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpExtractorTest {

    private final ClientIpExtractor extractor = new ClientIpExtractor();

    @Test
    void extractsSingleForwardedAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10");
        request.setRemoteAddr("10.0.0.4");

        assertThat(extractor.extract(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void extractsRenderPrependedAddressFromForwardedChain() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "198.51.100.7, 192.0.2.20, 10.0.0.4");
        request.setRemoteAddr("10.0.0.4");

        assertThat(extractor.extract(request)).isEqualTo("198.51.100.7");
    }

    @Test
    void fallsBackToRemoteAddressWithoutForwardedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.44");

        assertThat(extractor.extract(request)).isEqualTo("192.0.2.44");
    }
}
