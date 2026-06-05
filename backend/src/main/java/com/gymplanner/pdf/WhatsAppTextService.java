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
            out.append("*").append(dayHeader(i + 1, day.name())).append("*\n");
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

    /**
     * Construye el encabezado del día evitando duplicar "Día N".
     * Si el profesor nombró el día como "Día 1 - Circuito", mostramos
     * "DÍA 1 — Circuito". Si lo nombró "Potencia", mostramos
     * "DÍA 1 — Potencia". Si no le puso nombre o el nombre es solo
     * "Día N", mostramos solo "DÍA N".
     */
    private String dayHeader(int dayNumber, String customName) {
        String prefix = "DÍA " + dayNumber;
        if (!StringUtils.hasText(customName)) {
            return prefix;
        }
        String cleaned = stripDayPrefix(customName.trim(), dayNumber);
        if (cleaned.isEmpty()) {
            return prefix;
        }
        return prefix + " — " + cleaned;
    }

    /**
     * Quita prefijos como "Día 1", "Día 1 -", "Día 1:" del comienzo del
     * nombre custom. Case-insensitive y tolerante a espacios.
     */
    private String stripDayPrefix(String name, int dayNumber) {
        String lower = name.toLowerCase();
        String[] patterns = {
                "día " + dayNumber,
                "dia " + dayNumber,
                "day " + dayNumber,
        };
        for (String pattern : patterns) {
            if (lower.startsWith(pattern)) {
                String rest = name.substring(pattern.length()).trim();
                // Quita separadores comunes que vienen después del prefijo.
                while (rest.startsWith("-") || rest.startsWith(":") || rest.startsWith("—") || rest.startsWith("·")) {
                    rest = rest.substring(1).trim();
                }
                return rest;
            }
        }
        return name;
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
        // El typeLabel ahora viene null desde el PDF service porque ya
        // no se muestra en el PDF. WhatsApp toma la misma decisión: el
        // nombre del bloque + la nota de circuito (si aplica) alcanzan.
        boolean isCircuit = block.isCircuit();
        out.append("▶ *").append(block.title()).append("*\n");
        if (StringUtils.hasText(block.circuitNote())) {
            out.append("  ⏱ ").append(block.circuitNote()).append("\n");
        }
        if (StringUtils.hasText(block.blockNotes())) {
            out.append("  _").append(block.blockNotes()).append("_\n");
        }

        for (ExerciseGroup group : groupRows(block.rows())) {
            out.append("  • ").append(group.name()).append(" — ").append(formatSets(group.rows(), isCircuit)).append("\n");
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

    /**
     * Decide cómo concatenar los sets de un ejercicio.
     * - Circuito (1 sola "celda objetivo"): se imprime tal cual.
     * - 1 sola fila estándar colapsada: "3 series × 10 reps · 60 kg · 1 min".
     * - Múltiples filas (pirámide etc): "Serie 1: ... | Serie 2: ...".
     */
    private String formatSets(List<PdfExerciseRowDto> rows, boolean isCircuit) {
        if (rows.isEmpty()) return "-";
        if (rows.size() == 1) {
            return formatSingleRow(rows.getFirst(), isCircuit);
        }

        List<String> details = rows.stream().map(this::setDetails).toList();
        boolean allEqual = details.stream().distinct().count() == 1;
        boolean noneWithExecutionCue = rows.stream().noneMatch(row -> StringUtils.hasText(row.executionCue()));
        if (allEqual && !isCircuit && noneWithExecutionCue) {
            // Colapsamos en "N series × detalles".
            String first = rows.getFirst().cells().isEmpty() ? "" : rows.getFirst().cells().getFirst();
            String detailsStr = details.getFirst();
            return joinSeriesAndDetails(first, detailsStr, rows.size());
        }
        return formatExpanded(rows);
    }

    private String formatSingleRow(PdfExerciseRowDto row, boolean isCircuit) {
        if (row.cells().isEmpty()) {
            return "-";
        }

        // Circuito: una sola celda con todo el objetivo (ej "10 reps · 50 kg").
        if (isCircuit || row.cells().size() == 1) {
            return row.cells().getFirst();
        }

        String first = row.cells().getFirst();
        String details = setDetails(row);

        if (StringUtils.hasText(row.executionCue())) {
            return "Serie " + first + " · " + details;
        }

        // Standard colapsado: la primera celda es el número de series.
        // Ahora viene solo el número ("3"), antes era "3 series".
        // Reconstruimos: "3 series × 10 reps · 60 kg".
        return joinSeriesAndDetails(first, details, parseIntOr(first, 1));
    }

    /**
     * Une "N" + "10 reps · 60 kg" en "N series × 10 reps · 60 kg".
     * Si no hay detalles (ejercicios sin reps/peso/tiempo cargado),
     * devuelve solo "N series" sin el "×" colgando.
     */
    private String joinSeriesAndDetails(String firstCell, String details, int seriesCount) {
        String seriesText = seriesCount + (seriesCount == 1 ? " serie" : " series");
        if (!StringUtils.hasText(details) || details.equals("-")) {
            return seriesText;
        }
        return seriesText + " × " + details;
    }

    private String formatExpanded(List<PdfExerciseRowDto> rows) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) sb.append(" | ");
            String serieCell = rows.get(i).cells().isEmpty()
                    ? String.valueOf(i + 1)
                    : rows.get(i).cells().getFirst();
            if (StringUtils.hasText(rows.get(i).executionCue())) {
                sb.append("Serie ").append(serieCell).append(" · ").append(setDetails(rows.get(i)));
            } else {
                sb.append("Serie ").append(serieCell).append(": ").append(setDetails(rows.get(i)));
            }
        }
        return sb.toString();
    }

    /**
     * Toma una fila y devuelve los valores significativos concatenados
     * con " · ", saltándose la primera celda (que es el número de serie)
     * y los "-" o valores vacíos.
     *
     * Reconstruye palabras descriptivas para que sea legible en chat:
     * "10" en columna "Reps" → "10 reps"
     * "60 kg" en columna "Peso" → "60 kg"
     * "1 min" en columna "Descanso" → "descanso 1 min"
     */
    private String setDetails(PdfExerciseRowDto row) {
        if (row.cells().size() <= 1) return "-";

        List<String> labeledValues = new ArrayList<>();
        // El orden de columnas en cells es: [Serie/N, Reps, Peso?, Descanso] o variantes.
        // Trabajamos con índices porque las columnas dependen del MeasurementType.
        // Heurística simple: la última celda es siempre el descanso (cuando hay > 1).
        for (int i = 1; i < row.cells().size(); i++) {
            String cell = row.cells().get(i);
            if (!StringUtils.hasText(cell) || cell.equals("-")) continue;

            String labeled = labelCellByPosition(cell, i, row.cells().size());
            if (StringUtils.hasText(labeled)) {
                labeledValues.add(labeled);
            }
        }
        if (StringUtils.hasText(row.executionCue())) {
            labeledValues.add(row.executionCue().trim());
        }
        if (labeledValues.isEmpty()) return "-";
        return String.join(" · ", labeledValues);
    }

    /**
     * Asigna la palabra descriptiva a una celda según su posición en la fila.
     * Esto es necesario porque en el PDF las celdas vienen sin las palabras
     * (solo "10" en vez de "10 reps"), pero en WhatsApp necesitamos contexto.
     */
    private String labelCellByPosition(String cell, int index, int totalCells) {
        boolean isLast = index == totalCells - 1;

        // Última columna: siempre es Descanso.
        if (isLast) {
            return "descanso " + cell;
        }
        // Penúltima si hay >= 4 columnas: típicamente Peso/Tiempo/Distancia.
        // Estos valores ya vienen con su unidad ("60 kg", "30s", "100 m"), no agrego nada.
        if (totalCells >= 4 && index == totalCells - 2) {
            return cell;
        }
        // Resto: típicamente Reps. Si ya viene con "reps" o "al fallo", no agrego.
        if (cell.contains("reps") || cell.equals("al fallo")) {
            return cell;
        }
        // Si es un número solo, asumo que son reps.
        if (cell.matches("\\d+(-\\d+)?")) {
            return cell + " reps";
        }
        // Si tiene unidad propia (s, min, kg, m), lo dejo.
        return cell;
    }

    private int parseIntOr(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private record ExerciseGroup(String name, String notes, List<PdfExerciseRowDto> rows) {
    }
}
