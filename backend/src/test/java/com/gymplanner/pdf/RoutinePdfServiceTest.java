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
import com.gymplanner.routine.dto.RoutineBlockInput;
import com.gymplanner.routine.dto.RoutineDayInput;
import com.gymplanner.routine.dto.RoutineExerciseInput;
import com.gymplanner.routine.dto.RoutineExerciseSetInput;
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
    void pdf_keepsNonStandardMethodLabels() {
        Fixture fixture = fixture();

        String html = routinePdfService.renderHtml(fixture.routineId(), 1L);

        assertThat(html).contains("Circuito", "Pirámide");
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
    void pdf_collapsesIdenticalSets_inStandardBlock() throws Exception {
        Fixture fixture = fixture();

        String text = extractTextFromPdf(routinePdfService.generatePdf(fixture.routineId(), 1L));

        assertThat(text).contains("3 series");
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
                .contains("6 reps")
                .contains("80 kg")
                .contains("8 reps")
                .contains("70 kg")
                .contains("10 reps")
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

    private record Fixture(Long studentId, Long routineId) {
    }
}
