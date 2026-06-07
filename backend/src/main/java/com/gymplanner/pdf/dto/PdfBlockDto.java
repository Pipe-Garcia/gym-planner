package com.gymplanner.pdf.dto;

import com.gymplanner.shared.blocks.BlockStructuralType;
import java.util.List;

public record PdfBlockDto(
        String title,
        String typeLabel,
        BlockStructuralType structuralType,
        boolean isCircuit,
        boolean isGroupedSet,
        String circuitNote,
        String groupedSetNote,
        String roundsLabel,
        Integer targetRounds,
        Integer roundRestSeconds,
        String blockNotes,
        List<String> columns,
        List<PdfExerciseRowDto> rows) {
}
