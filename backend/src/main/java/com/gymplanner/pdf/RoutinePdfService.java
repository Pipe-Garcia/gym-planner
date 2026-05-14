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
        MeasurementType measurement = exercises.stream()
                .map(exercise -> exercise.getExercise().getDefaultMeasurement())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(MeasurementType.REPS_WEIGHT);

        boolean isCircuit = block.getStructuralType() == BlockStructuralType.CIRCUIT;
        boolean collapsed = !isCircuit
                && block.getStructuralType() == BlockStructuralType.STANDARD
                && exercises.stream().allMatch(exercise -> allSetsAreEquivalent(orderedSets(exercise)));

        List<String> columns;
        if (isCircuit) {
            columns = circuitColumns(measurement);
        } else if (collapsed) {
            columns = collapsedColumns(measurement);
        } else {
            columns = expandedColumns(measurement);
        }

        List<PdfExerciseRowDto> rows = new ArrayList<>();
        for (RoutineExercise exercise : exercises) {
            if (isCircuit) {
                rows.add(circuitRow(exercise, measurement));
            } else {
                rows.addAll(mapExerciseRows(exercise, measurement, collapsed));
            }
        }

        return new PdfBlockDto(
                block.getTitle(),
                typeLabel(block.getStructuralType()),
                isCircuit,
                circuitNote(block, exercises.size()),
                sanitizeNotes(block.getBlockNotes()),
                columns,
                rows);
    }

    private List<PdfExerciseRowDto> mapExerciseRows(RoutineExercise exercise, MeasurementType measurement, boolean collapsed) {
        List<RoutineExerciseSet> sets = orderedSets(exercise);
        String tagsLabel = "";
        String sanitizedNotes = sanitizeNotes(exercise.getExerciseNotes());

        if (sets.isEmpty()) {
            return List.of(new PdfExerciseRowDto(true, 1, exercise.getExercise().getName(), tagsLabel, sanitizedNotes, emptyCells(measurement, collapsed)));
        }

        if (collapsed) {
            return List.of(new PdfExerciseRowDto(true, 1, exercise.getExercise().getName(), tagsLabel, sanitizedNotes, collapsedCells(sets, measurement)));
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
                    expandedPdfCells(set, measurement)));
        }
        return rows;
    }

    /**
     * Renderiza un ejercicio dentro de un bloque CIRCUIT como una sola fila
     * con el target (reps/tiempo/distancia) y opcionalmente peso. Sin "Set",
     * sin "Series", sin "Descanso", porque en un circuito esos conceptos no
     * aplican: el orden es la rotación temporal del bloque entero.
     */
    private PdfExerciseRowDto circuitRow(RoutineExercise exercise, MeasurementType measurement) {
        List<RoutineExerciseSet> sets = orderedSets(exercise);
        String tagsLabel = "";
        String sanitizedNotes = sanitizeNotes(exercise.getExerciseNotes());
        if (sets.isEmpty()) {
            return new PdfExerciseRowDto(true, 1, exercise.getExercise().getName(), tagsLabel, sanitizedNotes, circuitEmptyCells(measurement));
        }
        RoutineExerciseSet ref = sets.getFirst();
        return new PdfExerciseRowDto(true, 1, exercise.getExercise().getName(), tagsLabel, sanitizedNotes, circuitCells(ref, measurement));
    }

    private List<RoutineExerciseSet> orderedSets(RoutineExercise exercise) {
        return exercise.getSets().stream()
                .sorted(Comparator.comparingInt(RoutineExerciseSet::getSetNumber))
                .toList();
    }

    private boolean allSetsAreEquivalent(List<RoutineExerciseSet> sets) {
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

    private List<String> collapsedCells(List<RoutineExerciseSet> sets, MeasurementType measurement) {
        RoutineExerciseSet ref = sets.getFirst();
        int count = sets.size();
        String seriesLabel = count + (count == 1 ? " serie" : " series");
        return switch (measurement) {
            case REPS_WEIGHT -> List.of(seriesLabel, repsLabel(ref), weightLabel(ref.getTargetWeightKg()), restLabel(ref.getRestAfterSeconds()));
            case REPS_ONLY, CIRCUIT_REPS -> List.of(seriesLabel, repsLabel(ref), restLabel(ref.getRestAfterSeconds()));
            case TIME -> List.of(seriesLabel, timeLabel(ref.getTargetTimeSeconds()), restLabel(ref.getRestAfterSeconds()));
            case DISTANCE -> List.of(seriesLabel, distanceLabel(ref.getTargetDistanceMeters()), restLabel(ref.getRestAfterSeconds()));
        };
    }

    private List<String> expandedCells(RoutineExerciseSet set, MeasurementType measurement) {
        return switch (measurement) {
            case REPS_WEIGHT -> List.of(setName(set), repsLabel(set), weightLabel(set.getTargetWeightKg()), restLabel(set.getRestAfterSeconds()));
            case REPS_ONLY, CIRCUIT_REPS -> List.of(setName(set), repsLabel(set), restLabel(set.getRestAfterSeconds()));
            case TIME -> List.of(setName(set), timeLabel(set.getTargetTimeSeconds()), restLabel(set.getRestAfterSeconds()));
            case DISTANCE -> List.of(setName(set), distanceLabel(set.getTargetDistanceMeters()), restLabel(set.getRestAfterSeconds()));
        };
    }

    private List<String> expandedPdfCells(RoutineExerciseSet set, MeasurementType measurement) {
        List<String> cells = new ArrayList<>(expandedCells(set, measurement));
        if (!cells.isEmpty()) {
            cells.set(0, serieLabel(set.getSetNumber()));
        }
        return cells;
    }

    private List<String> circuitCells(RoutineExerciseSet set, MeasurementType measurement) {
        return List.of(circuitObjectiveLabel(set, measurement));
    }

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

    private String circuitNote(RoutineBlock block, int exerciseCount) {
        if (block.getStructuralType() != BlockStructuralType.CIRCUIT || block.getTotalDurationSeconds() == null) {
            return null;
        }
        int minutes = Math.max(1, block.getTotalDurationSeconds() / 60);
        return "Rotar entre los " + exerciseCount + " ejercicios sin descanso durante " + minutes + " minutos. Volver al inicio al terminar.";
    }

    private String typeLabel(BlockStructuralType structuralType) {
        if (structuralType == null) return null;
        return switch (structuralType) {
            case STANDARD -> null;
            case CIRCUIT -> "Circuito";
            case PYRAMID -> "Pirámide";
            case REVERSE_PYRAMID -> "Pirámide inversa";
            case DROP_SET -> "Drop set";
            case REST_PAUSE -> "Rest pause";
            case CLUSTER -> "Cluster";
        };
    }

    private String firstText(String first, String second) {
        if (StringUtils.hasText(first)) return first;
        return StringUtils.hasText(second) ? second : null;
    }

    /**
     * Devuelve null si las notas vienen vacías, en blanco, o contienen
     * solo un guion. Esto evita renderizar "-" suelto en el PDF.
     */
    private String sanitizeNotes(String notes) {
        if (notes == null) return null;
        String trimmed = notes.trim();
        if (trimmed.isEmpty() || trimmed.equals("-") || trimmed.equals("--") || trimmed.equals("—")) {
            return null;
        }
        return trimmed;
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
