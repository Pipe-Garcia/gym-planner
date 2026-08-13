package com.gymplanner.auth;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimitDiagnostics {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int SECRET_BYTES = 32;
    private static final int FINGERPRINT_BYTES = 6;

    private final boolean enabled;
    private final SecretKeySpec sessionSecret;

    public LoginRateLimitDiagnostics(
            @Value("${gymplanner.security.login-rate-limit-diagnostics-enabled:false}") boolean enabled) {
        this.enabled = enabled;
        byte[] secret = new byte[SECRET_BYTES];
        new SecureRandom().nextBytes(secret);
        this.sessionSecret = new SecretKeySpec(secret, HMAC_ALGORITHM);
    }

    boolean isEnabled() {
        return enabled;
    }

    String fingerprint(String finalKey) {
        try {
            Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
            hmac.init(sessionSecret);
            byte[] digest = hmac.doFinal(finalKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, FINGERPRINT_BYTES);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Unable to calculate login rate-limit diagnostic fingerprint", exception);
        }
    }
}
