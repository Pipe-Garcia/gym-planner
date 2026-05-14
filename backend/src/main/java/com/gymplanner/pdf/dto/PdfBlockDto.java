package com.gymplanner.pdf.dto;

import java.util.List;

public record PdfBlockDto(
        String title,
        String typeLabel,
        boolean isCircuit,
        String circuitNote,
        String blockNotes,
        List<String> columns,
        List<PdfExerciseRowDto> rows) {
}
