package com.gymplanner.exercise;

import java.text.Normalizer;
import java.util.Locale;

final class SlugUtils {

    private SlugUtils() {
    }

    static String toSlug(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return normalized
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
