package com.gymplanner.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoginRateLimitDiagnosticsTest {

    @Test
    void fingerprintIsStableWithinOneJvmSessionAndContainsNoFinalKey() {
        LoginRateLimitDiagnostics diagnostics = new LoginRateLimitDiagnostics(true);
        String finalKey = "203.0.113.93";

        String first = diagnostics.fingerprint(finalKey);
        String second = diagnostics.fingerprint(finalKey);

        assertThat(first)
                .isEqualTo(second)
                .matches("[0-9a-f]{12}")
                .doesNotContain(finalKey);
        assertThat(diagnostics.fingerprint("198.51.100.93")).isNotEqualTo(first);
    }
}
