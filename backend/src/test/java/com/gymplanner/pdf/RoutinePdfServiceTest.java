package com.gymplanner.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymplanner.exercise.ExerciseService;
import com.gymplanner.exercise.MeasurementType;
import com.gymplanner.exercise.dto.CreateExerciseRequest;
import com.gymplanner.exercise.tag.ExerciseTagRepository;
import com.gymplanner.exercise.tag.TagType;
import com.gymplanner.routine.RoutineService;
import com.gymplanner.routine.RoutineStatus;
import com.gymplanner.routine.dto.CreateRoutineFromScratchRequest;
import com.gymplanner.routine.dto.FinishRoutineRequest;
import com.gymplanner.routine.dto.RoutineBlockInput;
import com.gymplanner.routine.dto.RoutineDayInput;
import com.gymplanner.routine.dto.RoutineExerciseInput;
import com.gymplanner.routine.dto.RoutineExerciseSetInput;
import com.gymplanner.routine.dto.RoutineResponse;
import com.gymplanner.shared.blocks.BlockPurpose;
import com.gymplanner.shared.blocks.BlockStructuralType;
import com.gymplanner.shared.blocks.SetKind;
import com.gymplanner.shared.exception.NotFoundException;
import com.gymplanner.student.Student;
import com.gymplanner.student.StudentRepository;
import com.gymplanner.student.StudentService;
import com.gymplanner.student.dto.CreateStudentRequest;
import com.gymplanner.student.injury.InjurySeverity;
import com.gymplanner.student.injury.StudentInjury;
import com.gymplanner.student.injury.StudentInjuryRepository;
import com.gymplanner.student.note.StudentNote;
import com.gymplanner.student.note.StudentNoteRepository;
import com.gymplanner.user.UserRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoutinePdfServiceTest {
    @Autowired RoutinePdfService routinePdfService;
    @Autowired RoutineService routineService;
    @Autowired StudentService studentService;
    @Autowired StudentRepository studentRepository;
    @Autowired ExerciseService exerciseService;
    @Autowired ExerciseTagRepository tagRepository;
    @Autowired StudentInjuryRepository injuryRepository;
    @Autowired StudentNoteRepository noteRepository;
    @Autowired UserRepository userRepository;

    @Test
    void pdf_doesNotContain_internalNotes() throws Exception {
        Fixture fixture = fixture();

        String text = extractTextFromPdf(routinePdfService.generatePdf(fixture.routineId(), 1L));

        assertThat(text).doesNotContain("Lesion en rodilla derecha");
        assertThat(text.toLowerCase()).doesNotContain("internal");
    }

    @Test
    void pdf_doesNotContain_closureNotes() throws Exception {
        String sentinel = "CLOSURE_SENTINEL_9f3a2c";
        Fixture fixture = fixture();

        routineService.finishRoutine(1L, 1L, fixture.routineId(), new FinishRoutineRequest(sentinel));
        RoutineResponse persisted = routineService.get(1L, fixture.routineId());
        assertThat(persisted.closureNotes()).isEqualTo(sentinel);

        String text = extractTextFromPdf(routinePdfService.generatePdf(fixture.routineId(), 1L));

        assertThat(text).doesNotContain(sentinel);
    }

    @Test
    void pdf_doesNotContain_studentInjuries() throws Exception {
        Fixture fixture = fixture();
        addInjury(fixture.studentId(), "Tobillo", "Esguince de tobillo");

        String text = extractTextFromPdf(routinePdfService.generatePdf(fixture.routineId(), 1L));

        assertThat(text).doesNotContain("Esguince", "tobillo");
    }

    @Test
    void pdf_doesNotContain_studentNotes() throws Exception {
        Fixture fixture = fixture();
        addStudentNote(fixture.studentId(), "Avisar al padre si falta");

        String text = extractTextFromPdf(routinePdfService.generatePdf(fixture.routineId(), 1L));

        assertThat(text).doesNotContain("Avisar al padre si falta");
    }

    @Test
    void pdf_includesGeneralNotes() throws Exception {
        Fixture fixture = fixture();

        String text = extractTextFromPdf(routinePdfService.generatePdf(fixture.routineId(), 1L));

        assertThat(text).contains("Tomar agua entre series");
    }

    @Test
    void pdf_groupsBlocksByCanonicalSection() {
        Fixture fixture = fixture();

        String html = routinePdfService.renderHtml(fixture.routineId(), 1L);

        assertThat(html).contains("CALENTAMIENTO", "PARTE PRINCIPAL", "VUELTA A LA CALMA");
    }

    @Test
    void pdf_showsGymInitialsWithoutExternalLogo() {
        Fixture fixture = fixture();
        Student student = studentRepository.findById(fixture.studentId()).orElseThrow();
        student.getGym().setName("Sergio Carrión Gym Extra");
        student.getGym().setLogoUrl("https://placehold.co/logo.png");

        String html = routinePdfService.renderHtml(fixture.routineId(), 1L);

        assertThat(html)
                .contains("class=\"gym-monogram\"")
                .contains(">SCG</div>")
                .doesNotContain("<img")
                .doesNotContain("https://placehold.co/logo.png");
    }

    @Test
    void pdf_doesNotShowExerciseTagsOrCategories() {
        Fixture fixture = fixtureWithTaggedExercise();

        String html = routinePdfService.renderHtml(fixture.routineId(), 1L);

        assertThat(html).contains("Banco control");
        assertThat(html)
                .doesNotContain("Core")
                .doesNotContain("Cuádriceps")
                .doesNotContain("Sentadilla ·")
                .doesNotContain("exercise-tags");
    }

    @Test
    void pdf_showsExerciseNotesAsStudentFacingNote() {
        Fixture fixture = fixtureWithTaggedExercise();

        String html = routinePdfService.renderHtml(fixture.routineId(), 1L);

        assertThat(html)
                .contains("Nota:")
                .contains("5 con cada pierna");
    }

    @Test
    void pdf_doesNotShowStandardBlockLabel() {
        Fixture fixture = fixture();

        String html = routinePdfService.renderHtml(fixture.routineId(), 1L);

        assertThat(html)
                .doesNotContain(">Estándar<")
                .doesNotContain("Estándar</span>");
    }

    @Test
    void pdf_doesNotShowStructuralTypeBadges() {
        Fixture fixture = fixture();

        String html = routinePdfService.renderHtml(fixture.routineId(), 1L);

        // Decisión de UX: el PDF ya no muestra badges de tipo estructural
        // ("Estándar", "Pirámide", "Circuito" al costado del título).
        // El alumno entiende qué hacer por el nombre del bloque y, si es
        // un circuito, por la nota destacada "Rotar entre los X ejercicios...".
        assertThat(html)
                .doesNotContain(">Estándar<")
                .doesNotContain(">Pirámide<")
                .doesNotContain(">Pirámide inversa<");

        // La nota explicativa del circuito sí debe seguir presente,
        // porque transmite información de ejecución que el alumno necesita.
        assertThat(html).contains("Rotar entre los");
    }

    @Test
    void pdf_circuitBlock_includesDurationNote() {
        Fixture fixture = fixture();

        String html = routinePdfService.renderHtml(fixture.routineId(), 1L);

        assertThat(html).contains("12 minutos");
    }

    @Test
    void pdf_pyramidBlock_listsAllSetsSeparately() {
        Fixture fixture = fixture();

        String html = routinePdfService.renderHtml(fixture.routineId(), 1L);

        assertThat(html).contains("80 kg", "70 kg", "60 kg");
    }

    @Test
    void pdf_collapsesIdenticalSets_inStandardBlock() {
        Fixture fixture = fixture();

        String html = routinePdfService.renderHtml(fixture.routineId(), 1L);

        // El bloque "Movilidad articular" tiene 3 sets idénticos en STANDARD.
        // Debe colapsarse a una sola fila con "3" en la columna SERIES.
        // Antes el test buscaba "3 series" en el texto del PDF; ahora la
        // celda solo dice "3" porque el header de columna ya transmite la unidad.
        assertThat(html)
                .contains("Movilidad articular")
                .contains(">Series<")        // header en plural = modo colapsado
                .contains("<td>3</td>");     // valor de la celda Series

        // Cross-check: el bloque colapsado NO debería tener "Primera serie",
        // "Segunda serie", etc. (eso sería el modo expandido).
        // Como el bloque de pirámide SÍ tiene esas etiquetas, no podemos
        // chequear ausencia global. Verificamos que el header del modo
        // expandido ("Serie" singular) coexista pero que "Series" plural
        // siga presente como prueba de que algún bloque colapsó.
    }

    @Test
    void pdf_collapsesIdenticalSetsWithoutExecutionCue() {
        Fixture fixture = fixtureWithSingleStandardBlock("Fuerza sin indicacion", identicalSets(new BigDecimal("20"), 12, 3));

        String html = routinePdfService.renderHtml(fixture.routineId(), 1L);

        assertThat(html)
                .contains("Fuerza sin indicacion")
                .contains(">Series<")
                .contains("<td>3</td>")
                .contains("<td>12</td>")
                .contains("20 kg")
                .doesNotContain("Primera serie")
                .doesNotContain("Segunda serie")
                .doesNotContain("Tercera serie");
    }

    @Test
    void pdf_expandsIdenticalSetsWithDifferentExecutionCue() {
        Fixture fixture = fixtureWithSingleStandardBlock("Curl parcial", List.of(
                set(12, new BigDecimal("20"), 60, "recorrido completo"),
                set(12, new BigDecimal("20"), 60, "parcial largo"),
                set(12, new BigDecimal("20"), 60, "parcial corto")));

        String html = routinePdfService.renderHtml(fixture.routineId(), 1L);

        assertThat(html)
                .contains("Curl parcial")
                .contains(">Serie<")
                .contains("Primera serie · recorrido completo")
                .contains("Segunda serie · parcial largo")
                .contains("Tercera serie · parcial corto");
    }

    @Test
    void pdf_standardBlockCollapsesPerExerciseWhenAnotherExerciseExpands() {
        Fixture fixture = fixtureWithMixedStandardBlock(
                List.of(
                        set(12, new BigDecimal("20"), 60, "recorrido completo"),
                        set(12, new BigDecimal("20"), 60, "parcial largo"),
                        set(12, new BigDecimal("20"), 60, "parcial corto")),
                identicalSets(new BigDecimal("35"), 10, 3));

        String html = routinePdfService.renderHtml(fixture.routineId(), 1L);

        assertThat(html)
                .contains("Fuerza mixta")
                .contains(">Serie<")
                .contains("Press Militar con Barra")
                .contains("Primera serie · recorrido completo")
                .contains("Segunda serie · parcial largo")
                .contains("Tercera serie · parcial corto")
                .contains("Remo con mancuerna")
                .contains("<td>3 series</td>")
                .contains("35 kg");
    }

    @Test
    void pdf_standardBlockWithAllExercisesEquivalentUsesCollapsedHeader() {
        Fixture fixture = fixtureWithMixedStandardBlock(
                identicalSets(new BigDecimal("20"), 12, 3),
                identicalSets(new BigDecimal("35"), 10, 3));

        String html = routinePdfService.renderHtml(fixture.routineId(), 1L);

        assertThat(html)
                .contains("Fuerza mixta")
                .contains(">Series<")
                .doesNotContain("Primera serie")
                .doesNotContain("<td>3 series</td>");
        assertThat(countOccurrences(html, "<td>3</td>")).isGreaterThanOrEqualTo(2);
    }

    @Test
    void pdf_expandsSets_inPyramidBlock() {
        Fixture fixture = fixture();

        String html = routinePdfService.renderHtml(fixture.routineId(), 1L);

        assertThat(html)
                .contains("Serie")
                .contains("Primera serie")
                .contains("Segunda serie")
                .contains("Tercera serie")
                // Las celdas de reps ya no llevan la palabra "reps":
                // el header de columna "REPS" ya transmite eso. Verificamos
                // los valores como celda exacta <td>N</td> para no confundir
                // con otros números que aparezcan en el HTML (como en "80 kg").
                .contains("<td>6</td>")
                .contains("80 kg")
                .contains("<td>8</td>")
                .contains("70 kg")
                .contains("<td>10</td>")
                .contains("60 kg");

        assertThat(html)
                .doesNotContain("S1")
                .doesNotContain("S2")
                .doesNotContain("S3")
                .doesNotContain(">Set<");
    }

    @Test
    void pdf_circuitBlock_usesObjectiveColumnWithoutSetColumn() {
        Fixture fixture = fixture();

        String html = routinePdfService.renderHtml(fixture.routineId(), 1L);

        assertThat(html)
                .contains("Circuito metabolico")
                .contains("Objetivo");

        assertThat(html)
                .doesNotContain("S1")
                .doesNotContain("S2")
                .doesNotContain("S3");
    }

    @Test
    void pdf_groupedSetBlock_includesRoundsRestNoteAndSimpleNumberedRows() {
        Fixture fixture = groupedSetFixture(3, 120);

        String html = routinePdfService.renderHtml(fixture.routineId(), 1L);

        assertThat(html)
                .contains("<div class=\"block-title\">Biserie Pecho</div>")
                .contains("3 vueltas. Sin descanso entre ejercicios. Descansar 120s al terminar cada vuelta.")
                .contains("1. Pecho Inclinado")
                .contains("2. Pecho Plano con Barra")
                .contains("8 reps")
                .contains("45 kg")
                .contains("40 kg")
                .doesNotContain("Biserie Pecho ·")
                .doesNotContain("A1")
                .doesNotContain("A2")
                .doesNotContain("A3");
    }

    @Test
    void pdf_groupedSetBlock_withoutRoundRestDoesNotIncludeRoundRestPhrase() {
        Fixture fixture = groupedSetFixture(1, null);

        String html = routinePdfService.renderHtml(fixture.routineId(), 1L);

        assertThat(html)
                .contains("<div class=\"block-title\">Biserie Pecho</div>")
                .contains("1 vuelta. Sin descanso entre ejercicios.")
                .doesNotContain("Biserie Pecho ·")
                .doesNotContain("al terminar cada vuelta");
    }

    @Test
    void pdf_groupedSetBlock_withZeroRoundRestDoesNotIncludeRoundRestPhrase() {
        Fixture fixture = groupedSetFixture(2, 0);

        String html = routinePdfService.renderHtml(fixture.routineId(), 1L);

        assertThat(html)
                .contains("<div class=\"block-title\">Biserie Pecho</div>")
                .contains("2 vueltas. Sin descanso entre ejercicios.")
                .doesNotContain("al terminar cada vuelta");
    }

    @Test
    void pdf_filename_includesStudentSlug() {
        Fixture fixture = fixture("María", "Pérez");

        String filename = routinePdfService.buildFilename(fixture.routineId(), 1L);

        assertThat(filename).isEqualTo("rutina_maria_perez_2026-05-12.pdf");
    }

    @Test
    void pdf_withoutLogo_rendersSuccessfully() {
        Fixture fixture = fixture();

        byte[] bytes = routinePdfService.generatePdf(fixture.routineId(), 1L);

        assertThat(bytes).isNotEmpty();
    }

    @Test
    void pdf_failsWith404_whenRoutineNotFound() {
        assertThatThrownBy(() -> routinePdfService.generatePdf(999_999L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Rutina no encontrada");
    }

    @Test
    void pdf_failsWith404_whenRoutineFromOtherGym() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> routinePdfService.generatePdf(fixture.routineId(), 2L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Rutina no encontrada");
    }

    private Fixture fixture() {
        return fixture("Martin", "Gomez");
    }

    private Fixture fixture(String firstName, String lastName) {
        Long studentId = studentService.create(1L, new CreateStudentRequest(
                firstName,
                lastName,
                null,
                "555",
                null,
                null,
                "Voley",
                "Saltabilidad",
                "Intermedio",
                null,
                LocalDate.now())).id();
        Long exerciseId = exerciseService.create(1L, new CreateExerciseRequest(
                "Sentadilla PDF " + System.nanoTime(),
                "Desc",
                null,
                MeasurementType.REPS_WEIGHT,
                null,
                null,
                List.of())).id();
        Long routineId = routineService.createFromScratch(1L, 1L, new CreateRoutineFromScratchRequest(
                studentId,
                "Plantilla Voley",
                "Saltabilidad",
                RoutineStatus.ACTIVE,
                LocalDate.of(2026, 5, 12),
                "Tomar agua entre series",
                "Lesion en rodilla derecha",
                List.of(day(exerciseId)))).id();
        return new Fixture(studentId, routineId);
    }

    private Fixture groupedSetFixture(Integer targetRounds, Integer roundRestSeconds) {
        Long studentId = studentService.create(1L, new CreateStudentRequest(
                "Bruno",
                "Grupo",
                null,
                "555",
                null,
                null,
                "Fuerza",
                "Hipertrofia",
                "Intermedio",
                null,
                LocalDate.now())).id();
        Long firstExerciseId = exerciseService.create(1L, new CreateExerciseRequest(
                "Pecho Inclinado",
                "Desc",
                null,
                MeasurementType.REPS_WEIGHT,
                null,
                null,
                List.of())).id();
        Long secondExerciseId = exerciseService.create(1L, new CreateExerciseRequest(
                "Pecho Plano con Barra",
                "Desc",
                null,
                MeasurementType.REPS_WEIGHT,
                null,
                null,
                List.of())).id();
        Long routineId = routineService.createFromScratch(1L, 1L, new CreateRoutineFromScratchRequest(
                studentId,
                "Rutina grouped set PDF",
                "Fuerza",
                RoutineStatus.ACTIVE,
                LocalDate.of(2026, 5, 12),
                null,
                "No publicar este texto interno",
                List.of(new RoutineDayInput(null, null, "Dia grouped", null, List.of(
                        block("Entrada grouped", BlockStructuralType.STANDARD, BlockPurpose.WARMUP, null, firstExerciseId, null, sets(new BigDecimal("0"), 8)),
                        groupedSetBlock(firstExerciseId, secondExerciseId, targetRounds, roundRestSeconds),
                        block("Salida grouped", BlockStructuralType.STANDARD, BlockPurpose.COOLDOWN, null, firstExerciseId, null, sets(new BigDecimal("0"), 8))
                ))))).id();
        return new Fixture(studentId, routineId);
    }

    private Fixture fixtureWithTaggedExercise() {
        Long studentId = studentService.create(1L, new CreateStudentRequest(
                "Lucia",
                "Salcedo",
                null,
                "555",
                null,
                null,
                "Fuerza",
                "Tecnica",
                "Intermedio",
                null,
                LocalDate.now())).id();
        Long exerciseId = exerciseService.create(1L, new CreateExerciseRequest(
                "Banco control " + System.nanoTime(),
                "Desc",
                null,
                MeasurementType.REPS_WEIGHT,
                null,
                null,
                List.of(
                        tagId(TagType.MUSCLE_GROUP, "Core"),
                        tagId(TagType.MUSCLE_GROUP, "Cuádriceps"),
                        tagId(TagType.MOVEMENT_PATTERN, "Sentadilla")))).id();
        Long routineId = routineService.createFromScratch(1L, 1L, new CreateRoutineFromScratchRequest(
                studentId,
                "Rutina limpia",
                "Fuerza",
                RoutineStatus.ACTIVE,
                LocalDate.of(2026, 5, 12),
                null,
                "No publicar este texto interno",
                List.of(day(exerciseId, "5 con cada pierna")))).id();
        return new Fixture(studentId, routineId);
    }

    private Fixture fixtureWithSingleStandardBlock(String blockTitle, List<RoutineExerciseSetInput> sets) {
        Long studentId = studentService.create(1L, new CreateStudentRequest(
                "Ana",
                "Cue",
                null,
                "555",
                null,
                null,
                "Fuerza",
                "Tecnica",
                "Intermedio",
                null,
                LocalDate.now())).id();
        Long exerciseId = exerciseService.create(1L, new CreateExerciseRequest(
                "Curl PDF " + System.nanoTime(),
                "Desc",
                null,
                MeasurementType.REPS_WEIGHT,
                null,
                null,
                List.of())).id();
        Long routineId = routineService.createFromScratch(1L, 1L, new CreateRoutineFromScratchRequest(
                studentId,
                "Rutina indicaciones PDF",
                "Fuerza",
                RoutineStatus.ACTIVE,
                LocalDate.of(2026, 5, 12),
                null,
                "No publicar este texto interno",
                List.of(new RoutineDayInput(null, null, "Dia unico", null, List.of(
                        block("Entrada tecnica", BlockStructuralType.STANDARD, BlockPurpose.WARMUP, null, exerciseId, null, sets(new BigDecimal("0"), 8)),
                        block(blockTitle, BlockStructuralType.STANDARD, BlockPurpose.MAIN_LIFT, null, exerciseId, null, sets),
                        block("Salida tecnica", BlockStructuralType.STANDARD, BlockPurpose.COOLDOWN, null, exerciseId, null, sets(new BigDecimal("0"), 8))
                ))))).id();
        return new Fixture(studentId, routineId);
    }

    private Fixture fixtureWithMixedStandardBlock(List<RoutineExerciseSetInput> firstExerciseSets, List<RoutineExerciseSetInput> secondExerciseSets) {
        Long studentId = studentService.create(1L, new CreateStudentRequest(
                "Ana",
                "Cue",
                null,
                "555",
                null,
                null,
                "Fuerza",
                "Tecnica",
                "Intermedio",
                null,
                LocalDate.now())).id();
        Long firstExerciseId = exerciseService.create(1L, new CreateExerciseRequest(
                "Press Militar con Barra",
                "Desc",
                null,
                MeasurementType.REPS_WEIGHT,
                null,
                null,
                List.of())).id();
        Long secondExerciseId = exerciseService.create(1L, new CreateExerciseRequest(
                "Remo con mancuerna",
                "Desc",
                null,
                MeasurementType.REPS_WEIGHT,
                null,
                null,
                List.of())).id();
        Long routineId = routineService.createFromScratch(1L, 1L, new CreateRoutineFromScratchRequest(
                studentId,
                "Rutina standard mixta",
                "Fuerza",
                RoutineStatus.ACTIVE,
                LocalDate.of(2026, 5, 12),
                null,
                "No publicar este texto interno",
                List.of(new RoutineDayInput(null, null, "Dia unico", null, List.of(
                        block("Entrada tecnica", BlockStructuralType.STANDARD, BlockPurpose.WARMUP, null, firstExerciseId, null, sets(new BigDecimal("0"), 8)),
                        new RoutineBlockInput(null, "Fuerza mixta", BlockStructuralType.STANDARD, BlockPurpose.MAIN_LIFT, null, null, null, List.of(
                                new RoutineExerciseInput(firstExerciseId, null, null, firstExerciseSets),
                                new RoutineExerciseInput(secondExerciseId, null, null, secondExerciseSets)
                        )),
                        block("Salida tecnica", BlockStructuralType.STANDARD, BlockPurpose.COOLDOWN, null, firstExerciseId, null, sets(new BigDecimal("0"), 8))
                ))))).id();
        return new Fixture(studentId, routineId);
    }

    private RoutineDayInput day(Long exerciseId) {
        return day(exerciseId, "Espalda neutra");
    }

    private RoutineDayInput day(Long exerciseId, String exerciseNotes) {
        return new RoutineDayInput(null, null, "Zona media", null, List.of(
                block("Movilidad articular", BlockStructuralType.STANDARD, BlockPurpose.WARMUP, null, exerciseId, exerciseNotes, identicalSets(new BigDecimal("0"), 10, 3)),
                block("Piramide principal", BlockStructuralType.PYRAMID, BlockPurpose.MAIN_LIFT, null, exerciseId, exerciseNotes, List.of(
                        set(6, new BigDecimal("80"), 90),
                        set(8, new BigDecimal("70"), 90),
                        set(10, new BigDecimal("60"), 90))),
                block("Circuito metabolico", BlockStructuralType.CIRCUIT, BlockPurpose.CONDITIONING, 720, exerciseId, exerciseNotes, identicalSets(new BigDecimal("0"), 15, 1)),
                block("Estiramiento", BlockStructuralType.STANDARD, BlockPurpose.COOLDOWN, null, exerciseId, exerciseNotes, sets(new BigDecimal("0"), 12))
        ));
    }

    private RoutineBlockInput block(String title, BlockStructuralType type, BlockPurpose purpose, Integer duration, Long exerciseId, String exerciseNotes, List<RoutineExerciseSetInput> sets) {
        return new RoutineBlockInput(null, title, type, purpose, duration, null, "Nota visible de bloque", List.of(
                new RoutineExerciseInput(exerciseId, null, exerciseNotes, sets)
        ));
    }

    private RoutineBlockInput groupedSetBlock(Long firstExerciseId, Long secondExerciseId, Integer targetRounds, Integer roundRestSeconds) {
        return new RoutineBlockInput(null, "Biserie Pecho", BlockStructuralType.GROUPED_SET, BlockPurpose.MAIN_LIFT, null, targetRounds, roundRestSeconds, null, List.of(
                new RoutineExerciseInput(firstExerciseId, null, null, List.of(set(8, new BigDecimal("45"), 0))),
                new RoutineExerciseInput(secondExerciseId, null, null, List.of(set(8, new BigDecimal("40"), 0)))
        ));
    }

    private Long tagId(TagType type, String name) {
        return tagRepository.findByGymIdAndTypeOrderByNameAsc(1L, type).stream()
                .filter(tag -> tag.getName().equals(name))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private List<RoutineExerciseSetInput> sets(BigDecimal weight, int reps) {
        return List.of(set(reps, weight, 60));
    }

    private List<RoutineExerciseSetInput> identicalSets(BigDecimal weight, int reps, int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> set(reps, weight, 60))
                .toList();
    }

    private RoutineExerciseSetInput set(int reps, BigDecimal weight, int restSeconds) {
        return new RoutineExerciseSetInput(null, SetKind.NORMAL, reps, null, null, weight, null, null, restSeconds, null, null, null, false);
    }

    private RoutineExerciseSetInput set(int reps, BigDecimal weight, int restSeconds, String executionCue) {
        return new RoutineExerciseSetInput(null, SetKind.NORMAL, reps, null, null, weight, null, null, restSeconds, null, executionCue, null, null, false);
    }

    private void addInjury(Long studentId, String bodyArea, String description) {
        Student student = studentRepository.findById(studentId).orElseThrow();
        StudentInjury injury = new StudentInjury();
        injury.setStudent(student);
        injury.setBodyArea(bodyArea);
        injury.setDescription(description);
        injury.setSeverity(InjurySeverity.MODERADA);
        injury.setActive(true);
        injuryRepository.save(injury);
    }

    private void addStudentNote(Long studentId, String content) {
        Student student = studentRepository.findById(studentId).orElseThrow();
        StudentNote note = new StudentNote();
        note.setStudent(student);
        note.setAuthorUser(userRepository.getReferenceById(1L));
        note.setContent(content);
        noteRepository.save(note);
    }

    private String extractTextFromPdf(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private int countOccurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private record Fixture(Long studentId, Long routineId) {
    }
}
