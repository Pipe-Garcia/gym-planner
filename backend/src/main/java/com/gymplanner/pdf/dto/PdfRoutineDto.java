package com.gymplanner.pdf.dto;

import java.util.List;

public record PdfRoutineDto(
        PdfMetadataDto metadata,
        String generalNotes,
        List<PdfDayDto> days) {
}
