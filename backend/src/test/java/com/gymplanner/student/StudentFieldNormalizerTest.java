package com.gymplanner.student;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StudentFieldNormalizerTest {

    private final StudentFieldNormalizer normalizer = new StudentFieldNormalizer();

    @Test
    void normalizesDniToDigits() {
        assertThat(normalizer.normalizeDni(" 12.345.678 ")).isEqualTo("12345678");
        assertThat(normalizer.normalizeDni(" - ")).isNull();
        assertThat(normalizer.normalizeDni(null)).isNull();
    }

    @Test
    void normalizesEmailWithTrimAndLowercase() {
        assertThat(normalizer.normalizeEmail(" A@X.COM ")).isEqualTo("a@x.com");
        assertThat(normalizer.normalizeEmail(" ")).isNull();
        assertThat(normalizer.normalizeEmail(null)).isNull();
    }
}
