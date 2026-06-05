package com.gymplanner.pdf.dto;

import java.util.List;

public record PdfExerciseRowDto(
        boolean spanRow,
        int rowspan,
        String exerciseName,
        String tagsLabel,
        String exerciseNotes,
        List<String> cells,
        List<String> pdfCells,
        String executionCue) {

    public PdfExerciseRowDto(
            boolean spanRow,
            int rowspan,
            String exerciseName,
            String tagsLabel,
            String exerciseNotes,
            List<String> cells) {
        this(spanRow, rowspan, exerciseName, tagsLabel, exerciseNotes, cells, cells, null);
    }

    public PdfExerciseRowDto(
            boolean spanRow,
            int rowspan,
            String exerciseName,
            String tagsLabel,
            String exerciseNotes,
            List<String> cells,
            List<String> pdfCells) {
        this(spanRow, rowspan, exerciseName, tagsLabel, exerciseNotes, cells, pdfCells, null);
    }
}
