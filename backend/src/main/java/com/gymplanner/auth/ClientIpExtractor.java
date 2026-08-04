package com.gymplanner.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ClientIpExtractor {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    public String extract(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (StringUtils.hasText(forwardedFor)) {
            /*
             * Render prepends the connecting client's address to X-Forwarded-For.
             * Production enables Spring's forwarded-header support only behind that
             * trusted proxy, so the first value is Render's trusted client address;
             * client-supplied values can only appear to its right.
             */
            String clientAddress = forwardedFor.split(",", 2)[0].trim();
            if (StringUtils.hasText(clientAddress)) {
                return clientAddress;
            }
        }

        return request.getRemoteAddr();
    }
}
