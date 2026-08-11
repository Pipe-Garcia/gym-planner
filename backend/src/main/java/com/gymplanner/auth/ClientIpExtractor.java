package com.gymplanner.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ClientIpExtractor {

    static final String UNKNOWN_CLIENT = "unknown-client";
    private static final String CF_CONNECTING_IP = "CF-Connecting-IP";

    private final ClientIpSource source;

    public ClientIpExtractor(
            @Value("${gymplanner.security.client-ip-source:remote-address}") String source) {
        this.source = ClientIpSource.from(source);
    }

    public String extract(HttpServletRequest request) {
        if (source == ClientIpSource.REMOTE_ADDRESS) {
            return request.getRemoteAddr();
        }

        /*
         * In production Cloudflare overwrites CF-Connecting-IP with one client IP.
         * X-Forwarded-For is deliberately ignored because a client-supplied first
         * value can survive the proxy chain. getRemoteAddr() is also ignored here:
         * Spring's ForwardedHeaderFilter may have derived it from that untrusted XFF.
         */
        String candidate = request.getHeader(CF_CONNECTING_IP);
        if (!StringUtils.hasText(candidate)) {
            return UNKNOWN_CLIENT;
        }

        candidate = candidate.trim();
        return isIpLiteral(candidate) ? candidate : UNKNOWN_CLIENT;
    }

    private boolean isIpLiteral(String candidate) {
        if (candidate.indexOf(',') >= 0 || candidate.indexOf('%') >= 0) {
            return false;
        }
        return candidate.indexOf(':') >= 0
                ? isIpv6Literal(candidate)
                : isIpv4Literal(candidate);
    }

    private boolean isIpv4Literal(String candidate) {
        String[] octets = candidate.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }

        for (String octet : octets) {
            if (octet.isEmpty() || octet.length() > 3 || (octet.length() > 1 && octet.charAt(0) == '0')) {
                return false;
            }
            for (int index = 0; index < octet.length(); index++) {
                char character = octet.charAt(index);
                if (character < '0' || character > '9') {
                    return false;
                }
            }
            if (Integer.parseInt(octet) > 255) {
                return false;
            }
        }
        return true;
    }

    private boolean isIpv6Literal(String candidate) {
        for (int index = 0; index < candidate.length(); index++) {
            char character = candidate.charAt(index);
            boolean hexadecimal = (character >= '0' && character <= '9')
                    || (character >= 'a' && character <= 'f')
                    || (character >= 'A' && character <= 'F');
            if (!hexadecimal && character != ':' && character != '.') {
                return false;
            }
        }

        try {
            // A colon plus the character whitelist guarantees no hostname/DNS lookup.
            InetAddress.getByName(candidate);
            return true;
        } catch (UnknownHostException exception) {
            return false;
        }
    }

    private enum ClientIpSource {
        CLOUDFLARE,
        REMOTE_ADDRESS;

        private static ClientIpSource from(String configuredSource) {
            try {
                return valueOf(configuredSource.trim().replace('-', '_').toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException | NullPointerException exception) {
                throw new IllegalArgumentException(
                        "Unsupported gymplanner.security.client-ip-source: " + configuredSource,
                        exception);
            }
        }
    }
}
