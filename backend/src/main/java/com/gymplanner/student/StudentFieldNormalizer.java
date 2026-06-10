package com.gymplanner.student;

import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StudentFieldNormalizer {

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
}
