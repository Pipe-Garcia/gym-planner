package com.gymplanner.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlugUtilsTest {

    @Test
    void removesAccentsAndLowercases() {
        assertThat(SlugUtils.toSlug("B\u00edceps")).isEqualTo("biceps");
        assertThat(SlugUtils.toSlug("F\u00fatbol")).isEqualTo("futbol");
    }

    @Test
    void replacesSpacesWithHyphens() {
        assertThat(SlugUtils.toSlug("Zona media")).isEqualTo("zona-media");
        assertThat(SlugUtils.toSlug("  Zona    media  ")).isEqualTo("zona-media");
    }

    @Test
    void collapsesSymbolsAndTrimsHyphens() {
        assertThat(SlugUtils.toSlug("*** Fuerza + potencia !!!")).isEqualTo("fuerza-potencia");
    }
}
