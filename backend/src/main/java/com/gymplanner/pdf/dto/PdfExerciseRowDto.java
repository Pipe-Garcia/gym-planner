package com.gymplanner.pdf.dto;

import java.util.List;

public record PdfExerciseRowDto(
        boolean spanRow,
        int rowspan,
        String exerciseName,
        String tagsLabel,
        String exerciseNotes,
        List<String> cells,
        List<String> pdfCells) {

    public PdfExerciseRowDto(
            boolean spanRow,
            int rowspan,
            String exerciseName,
            String tagsLabel,
            String exerciseNotes,
            List<String> cells) {
        this(spanRow, rowspan, exerciseName, tagsLabel, exerciseNotes, cells, cells);
    }
}
