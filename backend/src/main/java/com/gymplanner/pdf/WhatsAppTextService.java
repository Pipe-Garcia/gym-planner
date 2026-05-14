package com.gymplanner.pdf;

import com.gymplanner.pdf.dto.PdfBlockDto;
import com.gymplanner.pdf.dto.PdfDayDto;
import com.gymplanner.pdf.dto.PdfExerciseRowDto;
import com.gymplanner.pdf.dto.PdfRoutineDto;
import com.gymplanner.pdf.dto.PdfSectionDto;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WhatsAppTextService {
    private static final String SEP = "━━━━━━━━━━━━━━━━━━";

    private final RoutinePdfService routinePdfService;

    @Transactional(readOnly = true)
    public String generateText(Long routineId, Long gymId) {
        PdfRoutineDto data = routinePdfService.buildDto(routineId, gymId);
        StringBuilder out = new StringBuilder();

        out.append("*").append(data.metadata().gym().name()).append("*\n");
        out.append(SEP).append("\n\n");
        out.append("*").append(data.metadata().routineName()).append("*\n");
        out.append(data.metadata().studentFullName()).append(" · ")
                .append(data.metadata().assignedDateFormatted()).append("\n");
        appendObjectiveLine(out, data);
        out.append("\n");

        if (StringUtils.hasText(data.generalNotes())) {
            out.append("_").append(data.generalNotes()).append("_\n\n");
        }

        for (int i = 0; i < data.days().size(); i++) {
            PdfDayDto day = data.days().get(i);
            out.append(SEP).append("\n");
            out.append("*DÍA ").append(i + 1).append(" — ").append(day.name()).append("*\n");
            out.append(SEP).append("\n\n");

            for (PdfSectionDto section : day.sections()) {
                out.append("*").append(section.title()).append("*\n\n");
                for (PdfBlockDto block : section.blocks()) {
                    appendBlock(out, block);
                }
            }
        }

        out.append(SEP).append("\n");
        out.append(data.metadata().gym().name());
        return out.toString();
    }

    private void appendObjectiveLine(StringBuilder out, PdfRoutineDto data) {
        String objective = data.metadata().objective();
        String sport = data.metadata().sport();
        if (StringUtils.hasText(objective) || StringUtils.hasText(sport)) {
            out.append("Objetivo: ");
            if (StringUtils.hasText(objective)) out.append(objective);
            if (StringUtils.hasText(objective) && StringUtils.hasText(sport)) out.append(" · ");
            if (StringUtils.hasText(sport)) out.append(sport);
            out.append("\n");
        }
    }

    private void appendBlock(StringBuilder out, PdfBlockDto block) {
        boolean isStructural = isStructuralBlock(block.typeLabel());
        out.append("▶ *").append(block.title()).append("*");
        if (StringUtils.hasText(block.typeLabel())) {
            out.append(" _(").append(block.typeLabel()).append(")_");
        }
        out.append("\n");
        if (StringUtils.hasText(block.circuitNote())) {
            out.append("  ⏱ ").append(block.circuitNote()).append("\n");
        }
        if (StringUtils.hasText(block.blockNotes())) {
            out.append("  _").append(block.blockNotes()).append("_\n");
        }

        for (ExerciseGroup group : groupRows(block.rows())) {
            out.append("  • ").append(group.name()).append(" — ").append(formatSets(group.rows(), isStructural)).append("\n");
            if (StringUtils.hasText(group.notes())) {
                out.append("    _").append(group.notes()).append("_\n");
            }
        }
        out.append("\n");
    }

    private List<ExerciseGroup> groupRows(List<PdfExerciseRowDto> rows) {
        List<ExerciseGroup> groups = new ArrayList<>();
        ExerciseGroup current = null;
        for (PdfExerciseRowDto row : rows) {
            if (row.spanRow()) {
                current = new ExerciseGroup(row.exerciseName(), row.exerciseNotes(), new ArrayList<>());
                groups.add(current);
            }
            if (current != null) {
                current.rows().add(row);
            }
        }
        return groups;
    }

    private String formatSets(List<PdfExerciseRowDto> rows, boolean isStructural) {
        if (rows.isEmpty()) return "-";
        if (rows.size() == 1) {
            return formatSingleRow(rows.getFirst());
        }

        List<String> details = rows.stream().map(this::setDetails).toList();
        boolean allEqual = details.stream().distinct().count() == 1;
        if (isStructural) {
            return formatExpanded(rows);
        }
        if (allEqual) {
            return rows.size() + " series × " + details.getFirst();
        }
        return formatExpanded(rows);
    }

    private String formatSingleRow(PdfExerciseRowDto row) {
        if (row.cells().isEmpty()) {
            return "-";
        }

        String first = row.cells().getFirst();

        // Caso circuito: ahora el PDF DTO trae una sola celda "Objetivo"
        // Ej: "10 reps · 210 kg", "40s", "100 m".
        if (row.cells().size() == 1) {
            return first;
        }

        String details = setDetails(row);

        // Caso STANDARD colapsado:
        // "3 series" + "10 reps · 60 kg · descanso 1 min"
        if (first.toLowerCase().contains("serie")) {
            return first + " × " + details;
        }

        return details;
    }

    private String formatExpanded(List<PdfExerciseRowDto> rows) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) {
                sb.append(" | ");
            }

            String serie = rows.get(i).cells().isEmpty()
                    ? String.valueOf(i + 1)
                    : rows.get(i).cells().getFirst();

            sb.append("Serie ")
                    .append(serie)
                    .append(": ")
                    .append(setDetails(rows.get(i)));
        }

        return sb.toString();
    }

    private String setDetails(PdfExerciseRowDto row) {
        if (row.cells().size() <= 1) return "-";
        return String.join(" · ", row.cells().subList(1, row.cells().size()).stream()
                .filter(StringUtils::hasText)
                .filter(value -> !value.equals("-"))
                .toList());
    }

    private boolean isStructuralBlock(String typeLabel) {
        return StringUtils.hasText(typeLabel) && !"Circuito".equals(typeLabel);
    }

    private record ExerciseGroup(String name, String notes, List<PdfExerciseRowDto> rows) {
    }
}
