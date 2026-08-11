package com.gymplanner.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpExtractorTest {

    @Test
    void cloudflareSourceUsesValidConnectingIp() {
        ClientIpExtractor extractor = new ClientIpExtractor("cloudflare");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "203.0.113.10");
        request.setRemoteAddr("10.0.0.4");

        assertThat(extractor.extract(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void cloudflareSourceIgnoresSpoofedForwardedFor() {
        ClientIpExtractor extractor = new ClientIpExtractor("cloudflare");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "198.51.100.7");
        request.addHeader("X-Forwarded-For", "192.0.2.20, 10.0.0.4");
        request.setRemoteAddr("10.0.0.4");

        assertThat(extractor.extract(request)).isEqualTo("198.51.100.7");
    }

    @Test
    void cloudflareSourceUsesUnknownClientWhenHeaderIsAbsent() {
        ClientIpExtractor extractor = new ClientIpExtractor("cloudflare");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.44");

        assertThat(extractor.extract(request)).isEqualTo(ClientIpExtractor.UNKNOWN_CLIENT);
    }

    @Test
    void cloudflareSourceUsesUnknownClientForCommaSeparatedValue() {
        ClientIpExtractor extractor = new ClientIpExtractor("cloudflare");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "198.51.100.7, 192.0.2.20");

        assertThat(extractor.extract(request)).isEqualTo(ClientIpExtractor.UNKNOWN_CLIENT);
    }

    @Test
    void cloudflareSourceUsesUnknownClientForHostname() {
        ClientIpExtractor extractor = new ClientIpExtractor("cloudflare");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "attacker.example");

        assertThat(extractor.extract(request)).isEqualTo(ClientIpExtractor.UNKNOWN_CLIENT);
    }

    @Test
    void cloudflareSourceAcceptsIpv6Literal() {
        ClientIpExtractor extractor = new ClientIpExtractor("cloudflare");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "2001:db8::42");

        assertThat(extractor.extract(request)).isEqualTo("2001:db8::42");
    }

    @Test
    void remoteAddressSourceUsesRemoteAddress() {
        ClientIpExtractor extractor = new ClientIpExtractor("remote-address");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.44");

        assertThat(extractor.extract(request)).isEqualTo("192.0.2.44");
    }

    @Test
    void remoteAddressSourceIgnoresSpoofedForwardedFor() {
        ClientIpExtractor extractor = new ClientIpExtractor("remote-address");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "198.51.100.99");
        request.setRemoteAddr("192.0.2.44");

        assertThat(extractor.extract(request)).isEqualTo("192.0.2.44");
    }
}
