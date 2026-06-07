package com.gymplanner.pdf;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WhatsAppTextServiceTest {
    @Autowired WhatsAppTextService whatsAppTextService;
    @Autowired RoutineService routineService;
    @Autowired StudentService studentService;
    @Autowired StudentRepository studentRepository;
    @Autowired ExerciseService exerciseService;
    @Autowired ExerciseTagRepository tagRepository;
    @Autowired StudentInjuryRepository injuryRepository;
    @Autowired StudentNoteRepository noteRepository;
    @Autowired UserRepository userRepository;

    @Test
    void text_doesNotContain_internalNotes() {
        Fixture fixture = fixture();

        String text = whatsAppTextService.generateText(fixture.routineId(), 1L);

        assertThat(text).doesNotContain("Lesion en rodilla derecha");
    }

    @Test
    void text_doesNotContain_injuries() {
        Fixture fixture = fixture();
        addInjury(fixture.studentId(), "Tobillo", "Esguince de tobillo");

        String text = whatsAppTextService.generateText(fixture.routineId(), 1L);

        assertThat(text).doesNotContain("Esguince", "tobillo");
    }

    @Test
    void text_doesNotContain_studentNotes() {
        Fixture fixture = fixture();
        addStudentNote(fixture.studentId(), "Avisar al padre si falta");

        String text = whatsAppTextService.generateText(fixture.routineId(), 1L);

        assertThat(text).doesNotContain("Avisar al padre si falta");
    }

    @Test
    void text_includesGeneralNotes() {
        Fixture fixture = fixture();

        String text = whatsAppTextService.generateText(fixture.routineId(), 1L);

        assertThat(text).contains("Tomar agua entre series");
    }

    @Test
    void text_usesWhatsAppMarkdown() {
        Fixture fixture = fixture();

        String text = whatsAppTextService.generateText(fixture.routineId(), 1L);

        assertThat(text).contains("*Plantilla Voley*", "_Tomar agua entre series_");
    }

    @Test
    void text_groupsBlocksBySection() {
        Fixture fixture = fixture();

        String text = whatsAppTextService.generateText(fixture.routineId(), 1L);

        assertThat(text).contains("CALENTAMIENTO", "PARTE PRINCIPAL", "VUELTA A LA CALMA");
    }

    @Test
    void whatsapp_doesNotShowExerciseTagsOrCategories() {
        Fixture fixture = fixtureWithTaggedExercise();

        String text = whatsAppTextService.generateText(fixture.routineId(), 1L);

        assertThat(text).contains("Banco control");
        assertThat(text)
                .doesNotContain("Core")
                .doesNotContain("Cuádriceps")
                .doesNotContain("Sentadilla ·");
    }

    @Test
    void whatsapp_showsExerciseNotesInItalics() {
        Fixture fixture = fixtureWithTaggedExercise();

        String text = whatsAppTextService.generateText(fixture.routineId(), 1L);

        assertThat(text).contains("_5 con cada pierna_");
    }

    @Test
    void whatsapp_doesNotShowStandardBlockLabel() {
        Fixture fixture = fixture();

        String text = whatsAppTextService.generateText(fixture.routineId(), 1L);

        assertThat(text)
                .doesNotContain("_(Estándar)_")
                .doesNotContain("(Estándar)");
    }

    @Test
    void text_circuitBlock_hasRotationNote() {
        Fixture fixture = fixture();

        String text = whatsAppTextService.generateText(fixture.routineId(), 1L);

        assertThat(text).contains("Circuito metabolico", "Rotar");
    }

    @Test
    void whatsapp_collapsesIdenticalSets_inStandardBlock() {
        Fixture fixture = fixture();

        String text = whatsAppTextService.generateText(fixture.routineId(), 1L);

        assertThat(text).contains("3 series × 10 reps");
    }

    @Test
    void whatsapp_collapsesIdenticalSetsWithoutExecutionCue() {
        Fixture fixture = fixtureWithSingleStandardBlock("Fuerza sin indicacion", identicalSets(12, new BigDecimal("20"), 3));

        String text = whatsAppTextService.generateText(fixture.routineId(), 1L);

        assertThat(text)
                .contains("Fuerza sin indicacion")
                .contains("3 series × 12 reps · 20 kg")
                .doesNotContain("Serie 1 ·")
                .doesNotContain("recorrido completo");
    }

    @Test
    void whatsapp_expandsIdenticalSetsWithDifferentExecutionCue() {
        Fixture fixture = fixtureWithSingleStandardBlock("Curl parcial", List.of(
                set(12, new BigDecimal("20"), 60, "recorrido completo"),
                set(12, new BigDecimal("20"), 60, "parcial largo"),
                set(12, new BigDecimal("20"), 60, "parcial corto")));

        String text = whatsAppTextService.generateText(fixture.routineId(), 1L);

        assertThat(text).contains(
                "Serie 1 · 12 reps · 20 kg · descanso 1 min · recorrido completo",
                "Serie 2 · 12 reps · 20 kg · descanso 1 min · parcial largo",
                "Serie 3 · 12 reps · 20 kg · descanso 1 min · parcial corto"
        );
    }

    @Test
    void whatsapp_expandsSets_inPyramidBlock() {
        Fixture fixture = fixture();

        String text = whatsAppTextService.generateText(fixture.routineId(), 1L);

        assertThat(text).contains(
                "Serie 1: 6 reps · 80 kg",
                "Serie 2: 8 reps · 70 kg",
                "Serie 3: 10 reps · 60 kg"
        );

        assertThat(text)
                .doesNotContain("S1:")
                .doesNotContain("S2:")
                .doesNotContain("S3:");
    }

    @Test
    void whatsapp_circuitShowsObjectiveInsteadOfDash() {
        Fixture fixture = fixture();

        String text = whatsAppTextService.generateText(fixture.routineId(), 1L);

        assertThat(text)
                .contains("Circuito metabolico")
                .contains("10 reps")
                .contains("20 kg");

        assertThat(text).doesNotContain("— -");
    }

    @Test
    void whatsapp_groupedSetBlock_includesRoundsRestNoteAndSimpleNumberedRows() {
        Fixture fixture = groupedSetFixture(3, 120);

        String text = whatsAppTextService.generateText(fixture.routineId(), 1L);

        assertThat(text)
                .contains("▶ *Biserie Pecho*\n  3 vueltas. Sin descanso entre ejercicios. Descansar 120s al terminar cada vuelta.")
                .contains("1. Pecho Inclinado · 8 reps · 45 kg")
                .contains("2. Pecho Plano con Barra · 8 reps · 40 kg")
                .doesNotContain("▶ *Biserie Pecho* —")
                .doesNotContain("A1")
                .doesNotContain("A2")
                .doesNotContain("A3");
    }

    @Test
    void whatsapp_groupedSetBlock_withoutRoundRestDoesNotIncludeRoundRestPhrase() {
        Fixture fixture = groupedSetFixture(1, null);

        String text = whatsAppTextService.generateText(fixture.routineId(), 1L);

        assertThat(text)
                .contains("▶ *Biserie Pecho*\n  1 vuelta. Sin descanso entre ejercicios.")
                .doesNotContain("▶ *Biserie Pecho* —")
                .doesNotContain("al terminar cada vuelta");
    }

    @Test
    void whatsapp_groupedSetBlock_withZeroRoundRestDoesNotIncludeRoundRestPhrase() {
        Fixture fixture = groupedSetFixture(2, 0);

        String text = whatsAppTextService.generateText(fixture.routineId(), 1L);

        assertThat(text)
                .contains("▶ *Biserie Pecho*\n  2 vueltas. Sin descanso entre ejercicios.")
                .doesNotContain("al terminar cada vuelta");
    }

    @Test
    void whatsapp_doesNotIncludeDecorativeEmojis() {
        Fixture fixture = fixture();

        String text = whatsAppTextService.generateText(fixture.routineId(), 1L);

        assertThat(text).doesNotContain("🏋️", "📋", "👤", "📅", "🎯", "📝", "🔥", "🧘", "💪");
    }

    @Test
    void whatsapp_includesFunctionalSymbols() {
        Fixture fixture = fixture();

        String text = whatsAppTextService.generateText(fixture.routineId(), 1L);

        assertThat(text).contains("▶", "⏱", "━", "•");
    }

    private Fixture fixture() {
        Long studentId = studentService.create(1L, new CreateStudentRequest(
                "Martin",
                "Gomez",
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
                "Plancha WhatsApp " + System.nanoTime(),
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
                "Rutina grouped set WhatsApp",
                "Fuerza",
                RoutineStatus.ACTIVE,
                LocalDate.of(2026, 5, 12),
                null,
                "No publicar este texto interno",
                List.of(new RoutineDayInput(null, null, "Dia grouped", null, List.of(
                        block("Entrada grouped", BlockStructuralType.STANDARD, BlockPurpose.WARMUP, null, firstExerciseId, null, identicalSets(8, new BigDecimal("0"), 1)),
                        groupedSetBlock(firstExerciseId, secondExerciseId, targetRounds, roundRestSeconds),
                        block("Salida grouped", BlockStructuralType.STANDARD, BlockPurpose.COOLDOWN, null, firstExerciseId, null, identicalSets(8, new BigDecimal("0"), 1))
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
                "Curl WhatsApp " + System.nanoTime(),
                "Desc",
                null,
                MeasurementType.REPS_WEIGHT,
                null,
                null,
                List.of())).id();
        Long routineId = routineService.createFromScratch(1L, 1L, new CreateRoutineFromScratchRequest(
                studentId,
                "Rutina indicaciones WhatsApp",
                "Fuerza",
                RoutineStatus.ACTIVE,
                LocalDate.of(2026, 5, 12),
                null,
                "No publicar este texto interno",
                List.of(new RoutineDayInput(null, null, "Dia unico", null, List.of(
                        block("Entrada tecnica", BlockStructuralType.STANDARD, BlockPurpose.WARMUP, null, exerciseId, null, identicalSets(8, new BigDecimal("0"), 1)),
                        block(blockTitle, BlockStructuralType.STANDARD, BlockPurpose.MAIN_LIFT, null, exerciseId, null, sets),
                        block("Salida tecnica", BlockStructuralType.STANDARD, BlockPurpose.COOLDOWN, null, exerciseId, null, identicalSets(8, new BigDecimal("0"), 1))
                ))))).id();
        return new Fixture(studentId, routineId);
    }

    private RoutineDayInput day(Long exerciseId) {
        return day(exerciseId, "Espalda neutra");
    }

    private RoutineDayInput day(Long exerciseId, String exerciseNotes) {
        return new RoutineDayInput(null, null, "Zona media", null, List.of(
                block("Movilidad articular", BlockStructuralType.STANDARD, BlockPurpose.WARMUP, null, exerciseId, exerciseNotes, identicalSets(10, new BigDecimal("20"), 1)),
                block("Trabajo principal", BlockStructuralType.STANDARD, BlockPurpose.MAIN_LIFT, null, exerciseId, exerciseNotes, identicalSets(10, new BigDecimal("20"), 3)),
                block("Piramide", BlockStructuralType.PYRAMID, BlockPurpose.MAIN_LIFT, null, exerciseId, exerciseNotes, List.of(
                        set(6, new BigDecimal("80"), 90),
                        set(8, new BigDecimal("70"), 90),
                        set(10, new BigDecimal("60"), 90))),
                block("Circuito metabolico", BlockStructuralType.CIRCUIT, BlockPurpose.CONDITIONING, 720, exerciseId, exerciseNotes, identicalSets(10, new BigDecimal("20"), 1)),
                block("Estiramiento", BlockStructuralType.STANDARD, BlockPurpose.COOLDOWN, null, exerciseId, exerciseNotes, identicalSets(10, new BigDecimal("20"), 1))
        ));
    }

    private RoutineBlockInput block(String title, BlockStructuralType type, BlockPurpose purpose, Integer duration, Long exerciseId, String exerciseNotes, List<RoutineExerciseSetInput> sets) {
        return new RoutineBlockInput(null, title, type, purpose, duration, null, null, List.of(
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

    private List<RoutineExerciseSetInput> identicalSets(int reps, BigDecimal weight, int count) {
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

    private record Fixture(Long studentId, Long routineId) {
    }
}
