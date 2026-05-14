package com.gymplanner.pdf.dto;

import java.util.List;

public record PdfSectionDto(
        String kind,
        String title,
        String icon,
        List<PdfBlockDto> blocks) {
}
