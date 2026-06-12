package com.gymplanner.pdf;

import com.gymplanner.exercise.MeasurementType;
import com.gymplanner.gym.Gym;
import com.gymplanner.pdf.dto.PdfBlockDto;
import com.gymplanner.pdf.dto.PdfDayDto;
import com.gymplanner.pdf.dto.PdfExerciseRowDto;
import com.gymplanner.pdf.dto.PdfMetadataDto;
import com.gymplanner.pdf.dto.PdfRoutineDto;
import com.gymplanner.pdf.dto.PdfSectionDto;
import com.gymplanner.routine.Routine;
import com.gymplanner.routine.RoutineBlock;
import com.gymplanner.routine.RoutineDay;
import com.gymplanner.routine.RoutineExercise;
import com.gymplanner.routine.RoutineExerciseSet;
import com.gymplanner.routine.RoutineRepository;
import com.gymplanner.shared.blocks.BlockPurpose;
import com.gymplanner.shared.blocks.BlockStructuralType;
import com.gymplanner.shared.exception.NotFoundException;
import com.gymplanner.student.Student;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class RoutinePdfService {
    private static final Locale LOCALE = Locale.of("es", "AR");
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE);

    private final RoutineRepository routineRepository;
    private final PdfGenerator pdfGenerator;
    private final TemplateEngine thymeleafEngine;

    @Transactional(readOnly = true)
    public byte[] generatePdf(Long routineId, Long gymId) {
        return pdfGenerator.htmlToPdf(renderHtml(routineId, gymId));
    }

    @Transactional(readOnly = true)
    public String renderHtml(Long routineId, Long gymId) {
        PdfRoutineDto dto = buildDto(routineId, gymId);
        Context ctx = new Context(LOCALE);
        ctx.setVariable("data", dto);
        return thymeleafEngine.process("pdf/routine", ctx);
    }

    @Transactional(readOnly = true)
    public PdfRoutineDto buildDto(Long routineId, Long gymId) {
        return mapToPdfDto(loadRoutine(routineId, gymId));
    }

    @Transactional(readOnly = true)
    public String buildFilename(Long routineId, Long gymId) {
        return buildFilename(loadRoutine(routineId, gymId));
    }

    public String buildFilename(Routine routine) {
        Student student = routine.getStudent();
        String slug = slugify(student.getFirstName() + " " + student.getLastName());
        String date = routine.getAssignedDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return "rutina_" + slug + "_" + date + ".pdf";
    }

    private Routine loadRoutine(Long routineId, Long gymId) {
        return routineRepository.findByIdWithFullStructure(routineId, gymId)
                .orElseThrow(() -> new NotFoundException("Rutina no encontrada"));
    }

    private PdfRoutineDto mapToPdfDto(Routine routine) {
        Student student = routine.getStudent();
        Gym gym = student.getGym();
        PdfMetadataDto metadata = new PdfMetadataDto(
                new PdfMetadataDto.PdfGymDto(
                        gym.getName(),
                        gym.getOwnerName(),
                        gym.getPhone(),
                        gym.getEmail(),
                        gym.getAddress(),
                        StringUtils.hasText(gym.getPrimaryColor()) ? gym.getPrimaryColor() : "#2563EB",
                        initialsOf(gym.getName()),
                        gym.getLogoUrl()),
                routine.getName(),
                student.getFirstName() + " " + student.getLastName(),
                routine.getAssignedDate().format(DISPLAY_DATE),
                routine.getAssignedDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                firstText(routine.getObjective(), student.getObjective()),
                student.getSport());

        List<PdfDayDto> days = routine.getDays().stream()
                .sorted(Comparator.comparingInt(RoutineDay::getOrderIndex))
                .map(day -> new PdfDayDto(day.getName(), day.getNotes(), groupBySection(orderedBlocks(day))))
                .toList();

        return new PdfRoutineDto(metadata, routine.getGeneralNotes(), days);
    }

    private List<RoutineBlock> orderedBlocks(RoutineDay day) {
        return day.getBlocks().stream()
                .sorted(Comparator.comparingInt(RoutineBlock::getOrderIndex))
                .toList();
    }

    private List<PdfSectionDto> groupBySection(List<RoutineBlock> blocks) {
        Map<String, List<RoutineBlock>> grouped = new LinkedHashMap<>();
        grouped.put("warmup", new ArrayList<>());
        grouped.put("main", new ArrayList<>());
        grouped.put("cooldown", new ArrayList<>());

        for (RoutineBlock block : blocks) {
            String section = sectionOf(block.getPurpose());
            if (section != null) {
                grouped.get(section).add(block);
            }
        }

        List<PdfSectionDto> result = new ArrayList<>();
        addSection(result, grouped, "warmup", "CALENTAMIENTO", "🔥");
        addSection(result, grouped, "main", "PARTE PRINCIPAL", "🎯");
        addSection(result, grouped, "cooldown", "VUELTA A LA CALMA", "🧘");
        return result;
    }

    private void addSection(List<PdfSectionDto> result, Map<String, List<RoutineBlock>> grouped, String kind, String title, String icon) {
        List<RoutineBlock> blocks = grouped.get(kind);
        if (!blocks.isEmpty()) {
            result.add(new PdfSectionDto(kind, title, icon, blocks.stream().map(this::mapBlock).toList()));
        }
    }

    private String sectionOf(BlockPurpose purpose) {
        if (purpose == null) return null;
        return switch (purpose) {
            case WARMUP, ACTIVATION -> "warmup";
            case MAIN_LIFT, ACCESSORY, CONDITIONING, CORE, OTHER -> "main";
            case COOLDOWN -> "cooldown";
        };
    }

    private PdfBlockDto mapBlock(RoutineBlock block) {
        List<RoutineExercise> exercises = block.getExercises().stream()
                .sorted(Comparator.comparingInt(RoutineExercise::getOrderIndex))
                .toList();
        MeasurementType firstMeasurement = exercises.stream()
                .map(this::measurementOf)
                .findFirst()
                .orElse(MeasurementType.REPS_WEIGHT);

        boolean isCircuit = block.getStructuralType() == BlockStructuralType.CIRCUIT;
        boolean isGroupedSet = block.getStructuralType() == BlockStructuralType.GROUPED_SET;
        // Bloque mixto: tiene ejercicios con measurements distintos (e.g. plancha TIME + saltos REPS_ONLY).
        // En ese caso fusionamos las columnas variables en una sola "Objetivo" para que cada ejercicio
        // pueda mostrar su info sin perder datos por no encajar en la columna del primero.
        boolean isMixed = !isCircuit && !isGroupedSet && hasMixedMeasurements(exercises);
        boolean isStandard = block.getStructuralType() == BlockStructuralType.STANDARD;
        boolean isStandardTable = !isCircuit && !isGroupedSet && !isMixed && isStandard;
        boolean collapsed = !isCircuit
                && !isGroupedSet
                && isStandard
                && exercises.stream().allMatch(exercise -> allSetsAreEquivalent(orderedSets(exercise)));
        boolean standardHasExpandedExercise = isStandardTable && !collapsed;

        List<String> columns;
        if (isCircuit || isGroupedSet) {
            columns = circuitColumns(firstMeasurement);
        } else if (isMixed) {
            columns = mixedColumns(collapsed);
        } else if (isStandardTable && !standardHasExpandedExercise) {
            columns = collapsedColumns(firstMeasurement);
        } else {
            columns = expandedColumns(firstMeasurement);
        }

        List<PdfExerciseRowDto> rows = new ArrayList<>();
        for (int i = 0; i < exercises.size(); i++) {
            RoutineExercise exercise = exercises.get(i);
            if (isCircuit) {
                rows.add(circuitRow(exercise, firstMeasurement));
            } else if (isGroupedSet) {
                rows.add(groupedSetRow(exercise, firstMeasurement, i + 1));
            } else if (isMixed) {
                rows.addAll(mixedExerciseRows(exercise, collapsed));
            } else if (isStandardTable) {
                rows.addAll(mapStandardExerciseRows(exercise, firstMeasurement, standardHasExpandedExercise));
            } else {
                rows.addAll(mapExerciseRows(exercise, firstMeasurement, false));
            }
        }

        return new PdfBlockDto(
                block.getTitle(),
                typeLabel(block.getStructuralType()),
                block.getStructuralType(),
                isCircuit,
                isGroupedSet,
                circuitNote(block, exercises.size()),
                groupedSetNote(block),
                roundsLabel(block.getTargetRounds()),
                block.getTargetRounds(),
                block.getRoundRestSeconds(),
                sanitizeNotes(block.getBlockNotes()),
                columns,
                rows);
    }

    private List<PdfExerciseRowDto> mapStandardExerciseRows(RoutineExercise exercise, MeasurementType measurement, boolean blockHasExpandedExercise) {
        List<RoutineExerciseSet> sets = orderedSets(exercise);
        boolean exerciseCollapsed = allSetsAreEquivalent(sets);
        return mapExerciseRows(exercise, measurement, exerciseCollapsed, blockHasExpandedExercise && exerciseCollapsed);
    }

    private List<PdfExerciseRowDto> mapExerciseRows(RoutineExercise exercise, MeasurementType measurement, boolean collapsed) {
        return mapExerciseRows(exercise, measurement, collapsed, false);
    }

    private List<PdfExerciseRowDto> mapExerciseRows(RoutineExercise exercise, MeasurementType measurement, boolean collapsed, boolean collapsedInExpandedTable) {
        List<RoutineExerciseSet> sets = orderedSets(exercise);
        String tagsLabel = "";
        String sanitizedNotes = sanitizeNotes(exercise.getExerciseNotes());

        if (sets.isEmpty()) {
            return List.of(new PdfExerciseRowDto(true, 1, exercise.getExercise().getName(), tagsLabel, sanitizedNotes, emptyCells(measurement, collapsed)));
        }

        if (collapsed) {
            List<String> cells = collapsedCells(sets, measurement);
            List<String> pdfCells = collapsedInExpandedTable ? collapsedPdfCellsInExpandedTable(sets, measurement) : cells;
            return List.of(new PdfExerciseRowDto(true, 1, exercise.getExercise().getName(), tagsLabel, sanitizedNotes, cells, pdfCells));
        }

        int rowspan = sets.size();
        List<PdfExerciseRowDto> rows = new ArrayList<>();
        for (int i = 0; i < sets.size(); i++) {
            RoutineExerciseSet set = sets.get(i);
            rows.add(new PdfExerciseRowDto(
                    i == 0,
                    rowspan,
                    exercise.getExercise().getName(),
                    tagsLabel,
                    sanitizedNotes,
                    expandedCells(set, measurement),
                    expandedPdfCells(set, measurement),
                    normalizedExecutionCue(set)));
        }
        return rows;
    }

    private PdfExerciseRowDto circuitRow(RoutineExercise exercise, MeasurementType blockMeasurement) {
        List<RoutineExerciseSet> sets = orderedSets(exercise);
        String tagsLabel = "";
        String sanitizedNotes = sanitizeNotes(exercise.getExerciseNotes());

        MeasurementType exerciseMeasurement = exercise.getExercise().getDefaultMeasurement();
        if (exerciseMeasurement == null) {
            exerciseMeasurement = blockMeasurement;
        }

        if (sets.isEmpty()) {
            return new PdfExerciseRowDto(true, 1, exercise.getExercise().getName(), tagsLabel, sanitizedNotes, circuitEmptyCells(exerciseMeasurement));
        }
        RoutineExerciseSet ref = sets.getFirst();
        return new PdfExerciseRowDto(true, 1, exercise.getExercise().getName(), tagsLabel, sanitizedNotes, circuitCells(ref, exerciseMeasurement));
    }

    private PdfExerciseRowDto groupedSetRow(RoutineExercise exercise, MeasurementType blockMeasurement, int exerciseNumber) {
        PdfExerciseRowDto row = circuitRow(exercise, blockMeasurement);
        return new PdfExerciseRowDto(
                row.spanRow(),
                row.rowspan(),
                exerciseNumber + ". " + row.exerciseName(),
                row.tagsLabel(),
                row.exerciseNotes(),
                row.cells(),
                row.pdfCells(),
                row.executionCue());
    }

    private List<RoutineExerciseSet> orderedSets(RoutineExercise exercise) {
        return exercise.getSets().stream()
                .sorted(Comparator.comparingInt(RoutineExerciseSet::getSetNumber))
                .toList();
    }

    private boolean hasMixedMeasurements(List<RoutineExercise> exercises) {
        if (exercises.size() <= 1) return false;
        return exercises.stream()
                .map(this::measurementOf)
                .distinct()
                .count() > 1;
    }

    private MeasurementType measurementOf(RoutineExercise exercise) {
        MeasurementType m = exercise.getExercise().getDefaultMeasurement();
        return m == null ? MeasurementType.REPS_WEIGHT : m;
    }

    private boolean allSetsAreEquivalent(List<RoutineExerciseSet> sets) {
        if (sets.stream().anyMatch(set -> normalizedExecutionCue(set) != null)) {
            return false;
        }
        if (sets.size() <= 1) return true;
        RoutineExerciseSet first = sets.getFirst();
        return sets.stream().skip(1).allMatch(set ->
                Objects.equals(set.getTargetReps(), first.getTargetReps())
                        && Objects.equals(set.getTargetRepsMin(), first.getTargetRepsMin())
                        && Objects.equals(set.getTargetRepsMax(), first.getTargetRepsMax())
                        && Objects.equals(set.getTargetWeightKg(), first.getTargetWeightKg())
                        && Objects.equals(set.getTargetTimeSeconds(), first.getTargetTimeSeconds())
                        && Objects.equals(set.getTargetDistanceMeters(), first.getTargetDistanceMeters())
                        && Objects.equals(set.getRestAfterSeconds(), first.getRestAfterSeconds())
                        && !set.isToFailure()
                        && !first.isToFailure());
    }

    private List<String> collapsedColumns(MeasurementType measurement) {
        return switch (measurement) {
            case REPS_WEIGHT -> List.of("Series", "Reps", "Peso", "Descanso");
            case REPS_ONLY, CIRCUIT_REPS -> List.of("Series", "Reps", "Descanso");
            case TIME -> List.of("Series", "Tiempo", "Descanso");
            case DISTANCE -> List.of("Series", "Distancia", "Descanso");
        };
    }

    private List<String> expandedColumns(MeasurementType measurement) {
        return switch (measurement) {
            case REPS_WEIGHT -> List.of("Serie", "Reps", "Peso", "Descanso");
            case REPS_ONLY, CIRCUIT_REPS -> List.of("Serie", "Reps", "Descanso");
            case TIME -> List.of("Serie", "Tiempo", "Descanso");
            case DISTANCE -> List.of("Serie", "Distancia", "Descanso");
        };
    }

    /**
     * Columnas para bloques con measurements mixtos. La columna del medio
     * fusiona Reps/Tiempo/Distancia/Peso en una única "Objetivo".
     */
    private List<String> mixedColumns(boolean collapsed) {
        return collapsed
                ? List.of("Series", "Objetivo", "Descanso")
                : List.of("Serie", "Objetivo", "Descanso");
    }

    /**
     * Construye las filas para un ejercicio dentro de un bloque mixto.
     * Cada ejercicio usa su propio measurement para construir el "objetivo".
     * Reutiliza circuitObjectiveLabel porque hace exactamente lo que necesitamos:
     * "10 reps · 20 kg", "60s", "100 m", etc.
     */
    private List<PdfExerciseRowDto> mixedExerciseRows(RoutineExercise exercise, boolean collapsed) {
        List<RoutineExerciseSet> sets = orderedSets(exercise);
        String tagsLabel = "";
        String sanitizedNotes = sanitizeNotes(exercise.getExerciseNotes());
        MeasurementType measurement = measurementOf(exercise);

        if (sets.isEmpty()) {
            return List.of(new PdfExerciseRowDto(true, 1, exercise.getExercise().getName(), tagsLabel, sanitizedNotes, List.of("-", "-", "-")));
        }

        if (collapsed) {
            RoutineExerciseSet ref = sets.getFirst();
            int count = sets.size();
            String seriesLabel = String.valueOf(count);
            String objective = circuitObjectiveLabel(ref, measurement);
            String rest = plainTime(ref.getRestAfterSeconds());
            return List.of(new PdfExerciseRowDto(true, 1, exercise.getExercise().getName(), tagsLabel, sanitizedNotes, List.of(seriesLabel, objective, rest)));
        }

        int rowspan = sets.size();
        List<PdfExerciseRowDto> rows = new ArrayList<>();
        for (int i = 0; i < sets.size(); i++) {
            RoutineExerciseSet set = sets.get(i);
            String objective = circuitObjectiveLabel(set, measurement);
            String rest = plainTime(set.getRestAfterSeconds());
            List<String> cells = List.of(String.valueOf(set.getSetNumber()), objective, rest);
            List<String> pdfCells = List.of(serieLabel(set), objective, rest);
            rows.add(new PdfExerciseRowDto(
                    i == 0,
                    rowspan,
                    exercise.getExercise().getName(),
                    tagsLabel,
                    sanitizedNotes,
                    cells,
                    pdfCells,
                    normalizedExecutionCue(set)));
        }
        return rows;
    }

    /**
     * Columnas para circuito: solo el target. Sin Set/Series/Descanso porque
     * el circuito se ejecuta como rotación temporal del bloque.
     */
    private List<String> circuitColumns(MeasurementType measurement) {
        return List.of("Objetivo");
    }

    private List<String> emptyCells(MeasurementType measurement, boolean collapsed) {
        return (collapsed ? collapsedColumns(measurement) : expandedColumns(measurement)).stream().map(column -> "-").toList();
    }

    private List<String> circuitEmptyCells(MeasurementType measurement) {
        return circuitColumns(measurement).stream().map(column -> "-").toList();
    }

    /**
     * Modo colapsado (todas las series iguales): los headers de columna ya
     * dicen "Series", "Reps", "Peso", "Descanso", así que las celdas son
     * solo el valor sin la palabra repetida.
     */
    private List<String> collapsedCells(List<RoutineExerciseSet> sets, MeasurementType measurement) {
        RoutineExerciseSet ref = sets.getFirst();
        int count = sets.size();
        String seriesLabel = String.valueOf(count);
        return switch (measurement) {
            case REPS_WEIGHT -> List.of(seriesLabel, plainReps(ref), plainWeight(ref.getTargetWeightKg()), plainTime(ref.getRestAfterSeconds()));
            case REPS_ONLY, CIRCUIT_REPS -> List.of(seriesLabel, plainReps(ref), plainTime(ref.getRestAfterSeconds()));
            case TIME -> List.of(seriesLabel, plainTime(ref.getTargetTimeSeconds()), plainTime(ref.getRestAfterSeconds()));
            case DISTANCE -> List.of(seriesLabel, plainDistance(ref.getTargetDistanceMeters()), plainTime(ref.getRestAfterSeconds()));
        };
    }

    private List<String> collapsedPdfCellsInExpandedTable(List<RoutineExerciseSet> sets, MeasurementType measurement) {
        List<String> cells = new ArrayList<>(collapsedCells(sets, measurement));
        if (!cells.isEmpty()) {
            int count = sets.size();
            cells.set(0, count + (count == 1 ? " serie" : " series"));
        }
        return cells;
    }

    /**
     * Modo expandido (pirámide, drop set, etc.): los headers son "Serie",
     * "Reps", "Peso", "Descanso". Las celdas usan los valores sin las
     * palabras repetidas. La columna "Serie" sí lleva el nombre completo
     * ("Primera serie") para que se lea natural.
     */
    private List<String> expandedCells(RoutineExerciseSet set, MeasurementType measurement) {
        return switch (measurement) {
            case REPS_WEIGHT -> List.of(setName(set), plainReps(set), plainWeight(set.getTargetWeightKg()), plainTime(set.getRestAfterSeconds()));
            case REPS_ONLY, CIRCUIT_REPS -> List.of(setName(set), plainReps(set), plainTime(set.getRestAfterSeconds()));
            case TIME -> List.of(setName(set), plainTime(set.getTargetTimeSeconds()), plainTime(set.getRestAfterSeconds()));
            case DISTANCE -> List.of(setName(set), plainDistance(set.getTargetDistanceMeters()), plainTime(set.getRestAfterSeconds()));
        };
    }

    private List<String> expandedPdfCells(RoutineExerciseSet set, MeasurementType measurement) {
        List<String> cells = new ArrayList<>(expandedCells(set, measurement));
        if (!cells.isEmpty()) {
            cells.set(0, serieLabel(set));
        }
        return cells;
    }

    private List<String> circuitCells(RoutineExerciseSet set, MeasurementType measurement) {
        return List.of(circuitObjectiveLabel(set, measurement));
    }

    /**
     * En circuito sí mantenemos las palabras ("8 reps", "1 min", etc.)
     * porque la columna única "Objetivo" no contextualiza qué es cada
     * valor — sin las palabras, "8 · 20 kg" sería ambiguo.
     */
    private String circuitObjectiveLabel(RoutineExerciseSet set, MeasurementType measurement) {
        return switch (measurement) {
            case REPS_WEIGHT -> joinNonEmpty(repsLabel(set), weightLabel(set.getTargetWeightKg()));
            case REPS_ONLY, CIRCUIT_REPS -> repsLabel(set);
            case TIME -> timeLabel(set.getTargetTimeSeconds());
            case DISTANCE -> distanceLabel(set.getTargetDistanceMeters());
        };
    }

    private String joinNonEmpty(String first, String second) {
        boolean hasFirst = StringUtils.hasText(first) && !first.equals("-");
        boolean hasSecond = StringUtils.hasText(second) && !second.equals("-");

        if (hasFirst && hasSecond) return first + " · " + second;
        if (hasFirst) return first;
        if (hasSecond) return second;
        return "-";
    }

    private String setName(RoutineExerciseSet set) {
        return String.valueOf(set.getSetNumber());
    }

    private String serieLabel(Integer setNumber) {
        if (setNumber == null) return "Serie";
        return switch (setNumber) {
            case 1 -> "Primera serie";
            case 2 -> "Segunda serie";
            case 3 -> "Tercera serie";
            case 4 -> "Cuarta serie";
            case 5 -> "Quinta serie";
            case 6 -> "Sexta serie";
            case 7 -> "Séptima serie";
            case 8 -> "Octava serie";
            case 9 -> "Novena serie";
            case 10 -> "Décima serie";
            default -> "Serie " + setNumber;
        };
    }

    private String serieLabel(RoutineExerciseSet set) {
        String label = serieLabel(set.getSetNumber());
        String executionCue = normalizedExecutionCue(set);
        return executionCue == null ? label : label + " · " + executionCue;
    }

    private String normalizedExecutionCue(RoutineExerciseSet set) {
        return normalizedExecutionCue(set.getExecutionCue());
    }

    private String normalizedExecutionCue(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /* ── Labels "con palabras" — usados en circuito y WhatsApp ─────────── */

    private String repsLabel(RoutineExerciseSet set) {
        if (set.isToFailure()) return "al fallo";
        if (set.getTargetReps() != null) return set.getTargetReps() + " reps";
        if (set.getTargetRepsMin() != null && set.getTargetRepsMax() != null) {
            return set.getTargetRepsMin() + "-" + set.getTargetRepsMax() + " reps";
        }
        return "-";
    }

    private String timeLabel(Integer seconds) {
        if (seconds == null) return "-";
        if (seconds % 60 == 0) return (seconds / 60) + " min";
        return seconds + "s";
    }

    private String restLabel(Integer seconds) {
        if (seconds == null) return "-";
        return "descanso " + timeLabel(seconds);
    }

    private String weightLabel(BigDecimal weight) {
        if (weight == null) return "-";
        return trim(weight) + " kg";
    }

    private String distanceLabel(BigDecimal meters) {
        if (meters == null) return "-";
        return trim(meters) + " m";
    }

    /* ── Labels "sin palabras" — usados en celdas de tabla del PDF ─────── */

    private String plainReps(RoutineExerciseSet set) {
        if (set.isToFailure()) return "al fallo";
        if (set.getTargetReps() != null) return String.valueOf(set.getTargetReps());
        if (set.getTargetRepsMin() != null && set.getTargetRepsMax() != null) {
            return set.getTargetRepsMin() + "-" + set.getTargetRepsMax();
        }
        return "-";
    }

    private String plainTime(Integer seconds) {
        if (seconds == null) return "-";
        if (seconds % 60 == 0) return (seconds / 60) + " min";
        return seconds + "s";
    }

    private String plainWeight(BigDecimal weight) {
        if (weight == null) return "-";
        return trim(weight) + " kg";
    }

    private String plainDistance(BigDecimal meters) {
        if (meters == null) return "-";
        return trim(meters) + " m";
    }

    private String circuitNote(RoutineBlock block, int exerciseCount) {
        if (block.getStructuralType() != BlockStructuralType.CIRCUIT || block.getTotalDurationSeconds() == null) {
            return null;
        }
        int minutes = Math.max(1, block.getTotalDurationSeconds() / 60);
        return "Rotar entre los " + exerciseCount + " ejercicios sin descanso durante " + minutes + " minutos.";
    }

    private String groupedSetNote(RoutineBlock block) {
        if (block.getStructuralType() != BlockStructuralType.GROUPED_SET) {
            return null;
        }
        String note = roundsLabel(block.getTargetRounds()) + ". Sin descanso entre ejercicios.";
        Integer roundRestSeconds = block.getRoundRestSeconds();
        if (roundRestSeconds != null && roundRestSeconds > 0) {
            note += " Descansar " + roundRestLabel(roundRestSeconds) + " al terminar cada vuelta.";
        }
        return note;
    }

    private String roundsLabel(Integer targetRounds) {
        int rounds = targetRounds == null ? 1 : targetRounds;
        return rounds + (rounds == 1 ? " vuelta" : " vueltas");
    }

    private String roundRestLabel(Integer seconds) {
        return seconds + "s";
    }

    /**
     * El tipo estructural se mantiene en el DTO para uso interno
     * (WhatsApp puede seguir usándolo si tiene sentido). Pero en el PDF
     * ya no se muestra como badge: el nombre del bloque y, si aplica, la
     * nota de circuito, alcanzan para que el alumno entienda qué hacer.
     */
    private String typeLabel(BlockStructuralType structuralType) {
        return null;
    }

    private String firstText(String first, String second) {
        if (StringUtils.hasText(first)) return first;
        return StringUtils.hasText(second) ? second : null;
    }

    private String sanitizeNotes(String notes) {
        if (notes == null) return null;
        String trimmed = notes.trim();
        if (trimmed.isEmpty() || trimmed.equals("-") || trimmed.equals("--") || trimmed.equals("—")) {
            return null;
        }
        return trimmed;
    }

    private String initialsOf(String name) {
        if (!StringUtils.hasText(name)) return "G";

        StringBuilder initials = new StringBuilder(3);
        for (String word : name.trim().split("\\s+")) {
            if (word.isEmpty()) continue;
            initials.appendCodePoint(Character.toUpperCase(word.codePointAt(0)));
            if (initials.codePointCount(0, initials.length()) == 3) break;
        }
        return initials.isEmpty() ? "G" : initials.toString();
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (!StringUtils.hasText(normalized)) {
            normalized = "alumno";
        }
        return normalized.length() > 50 ? normalized.substring(0, 50) : normalized;
    }

    private String trim(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
