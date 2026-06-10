package com.gymplanner.student;

import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StudentFieldNormalizer {

    private static final String ARGENTINA_COUNTRY_CODE = "54";
    private static final String ARGENTINA_MOBILE_PREFIX = "9";
    private static final int ARGENTINA_NATIONAL_NUMBER_LENGTH = 10;

    public String normalizeDni(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[^0-9]", "");
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    public String normalizeEmail(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public String normalizePhone(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }

        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        }
        if (digits.startsWith(ARGENTINA_COUNTRY_CODE)) {
            digits = digits.substring(ARGENTINA_COUNTRY_CODE.length());
        }
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        if (digits.startsWith(ARGENTINA_MOBILE_PREFIX)
                && digits.length() == ARGENTINA_NATIONAL_NUMBER_LENGTH + 1) {
            digits = digits.substring(1);
        }

        if (digits.length() == ARGENTINA_NATIONAL_NUMBER_LENGTH + 2) {
            for (int areaCodeLength = 2; areaCodeLength <= 4; areaCodeLength++) {
                if (digits.substring(areaCodeLength, areaCodeLength + 2).equals("15")) {
                    digits = digits.substring(0, areaCodeLength) + digits.substring(areaCodeLength + 2);
                    break;
                }
            }
        }

        if (digits.length() != ARGENTINA_NATIONAL_NUMBER_LENGTH || hasRepeatedSingleDigit(digits)) {
            return null;
        }
        return ARGENTINA_COUNTRY_CODE + ARGENTINA_MOBILE_PREFIX + digits;
    }

    private boolean hasRepeatedSingleDigit(String digits) {
        char firstDigit = digits.charAt(0);
        return digits.chars().allMatch(digit -> digit == firstDigit);
    }
}
