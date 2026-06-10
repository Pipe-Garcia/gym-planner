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

    @Test
    void normalizesArgentinePhoneForMatching() {
        assertThat(normalizer.normalizePhone("011 15-2345-6789")).isEqualTo("5491123456789");
        assertThat(normalizer.normalizePhone("+54 9 11 2345-6789")).isEqualTo("5491123456789");
        assertThat(normalizer.normalizePhone("1111111111")).isNull();
        assertThat(normalizer.normalizePhone("123")).isNull();
        assertThat(normalizer.normalizePhone(null)).isNull();
    }
}
