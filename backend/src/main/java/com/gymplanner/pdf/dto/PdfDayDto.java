package com.gymplanner.pdf.dto;

import java.util.List;

public record PdfDayDto(
        String name,
        String notes,
        List<PdfSectionDto> sections) {
}
