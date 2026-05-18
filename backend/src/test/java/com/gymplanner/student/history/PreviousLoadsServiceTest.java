package com.gymplanner.student.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymplanner.exercise.ExerciseService;
import com.gymplanner.exercise.MeasurementType;
import com.gymplanner.exercise.dto.CreateExerciseRequest;
import com.gymplanner.routine.Routine;
import com.gymplanner.routine.RoutineRepository;
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
import com.gymplanner.student.history.dto.PreviousLoadsResponse;
import com.gymplanner.student.injury.InjurySeverity;
import com.gymplanner.student.injury.StudentInjury;
import com.gymplanner.student.injury.StudentInjuryRepository;
import com.gymplanner.student.note.StudentNote;
import com.gymplanner.student.note.StudentNoteRepository;
import com.gymplanner.user.UserRepository;
import jakarta.persistence.EntityManager;
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
class PreviousLoadsServiceTest {
    @Autowired PreviousLoadsService previousLoadsService;
    @Autowired StudentService studentService;
    @Autowired StudentRepository studentRepository;
    @Autowired ExerciseService exerciseService;
    @Autowired RoutineService routineService;
    @Autowired RoutineRepository routineRepository;
    @Autowired StudentInjuryRepository injuryRepository;
    @Autowired StudentNoteRepository noteRepository;
    @Autowired UserRepository userRepository;
    @Autowired EntityManager entityManager;
    @Autowired ObjectMapper objectMapper;

    @Test
    void returnsLatestOccurrenceForStudentAndExercise() {
        Fixture fixture = fixture();
        Long routineId = createRoutine(fixture, "Plantilla Voley - Potencia", RoutineStatus.FINISHED,
                LocalDate.of(2026, 5, 12), LocalDate.of(2026, 6, 12));

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(1L, fixture.studentId(), fixture.exerciseId(), null, 1);

        assertThat(response.found()).isTrue();
        assertThat(response.exerciseId()).isEqualTo(fixture.exerciseId());
        assertThat(response.exerciseName()).startsWith("Sentadilla Goblet");
        assertThat(response.occurrences()).hasSize(1);
        assertThat(response.occurrences().getFirst().routineId()).isEqualTo(routineId);
        assertThat(response.occurrences().getFirst().routineStatus()).isEqualTo(RoutineStatus.FINISHED);
        assertThat(response.occurrences().getFirst().blockStructuralType()).isEqualTo(BlockStructuralType.REVERSE_PYRAMID);
        assertThat(response.occurrences().getFirst().blockPurpose()).isEqualTo(BlockPurpose.MAIN_LIFT);
        assertThat(response.occurrences().getFirst().sets()).hasSize(1);
        assertThat(response.occurrences().getFirst().sets().getFirst().targetWeightKg()).isEqualByComparingTo("80");
    }

    @Test
    void returnsMultipleOccurrencesSortedByEffectiveDate() {
        Fixture fixture = fixture();
        Long oldest = createRoutine(fixture, "Old", RoutineStatus.FINISHED, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 10));
        Long middle = createRoutine(fixture, "Middle", RoutineStatus.FINISHED, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 10));
        Long latest = createRoutine(fixture, "Active", RoutineStatus.ACTIVE, LocalDate.of(2026, 6, 1), null);

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(1L, fixture.studentId(), fixture.exerciseId(), null, 3);

        assertThat(response.occurrences())
                .extracting("routineId")
                .containsExactly(latest, middle, oldest);
    }

    @Test
    void respectsLimitParam() {
        Fixture fixture = fixture();
        createRoutines(fixture, 5);

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(1L, fixture.studentId(), fixture.exerciseId(), null, 2);

        assertThat(response.occurrences()).hasSize(2);
    }

    @Test
    void capsLimitAt3() {
        Fixture fixture = fixture();
        createRoutines(fixture, 5);

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(1L, fixture.studentId(), fixture.exerciseId(), null, 99);

        assertThat(response.occurrences()).hasSize(3);
    }

    @Test
    void defaultsLimitTo1WhenNotProvided() {
        Fixture fixture = fixture();
        createRoutines(fixture, 3);

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(1L, fixture.studentId(), fixture.exerciseId(), null, null);

        assertThat(response.occurrences()).hasSize(1);
    }

    @Test
    void ignoresDraftRoutines() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Draft", RoutineStatus.DRAFT, LocalDate.of(2026, 6, 1), null);

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(1L, fixture.studentId(), fixture.exerciseId(), null, 3);

        assertThat(response.found()).isFalse();
        assertThat(response.occurrences()).isEmpty();
    }

    @Test
    void respectsExcludeRoutineId() {
        Fixture fixture = fixture();
        Long older = createRoutine(fixture, "Older", RoutineStatus.FINISHED, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 1));
        Long newer = createRoutine(fixture, "Newer", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(1L, fixture.studentId(), fixture.exerciseId(), newer, 3);

        assertThat(response.occurrences()).extracting("routineId").containsExactly(older);
    }

    @Test
    void returnsPreviousLoadsFilteredByStructuralType() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Standard", RoutineStatus.FINISHED, LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 1), null, null, BlockStructuralType.STANDARD,
                BlockPurpose.MAIN_LIFT, List.of(set(10, "45", 60)));
        Long pyramid = createRoutine(fixture, "Pyramid", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1), null, null, BlockStructuralType.PYRAMID,
                BlockPurpose.MAIN_LIFT, List.of(set(12, "30", 90), set(10, "35", 90)));

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(
                1L, fixture.studentId(), fixture.exerciseId(), null, BlockStructuralType.PYRAMID, 3);

        assertThat(response.found()).isTrue();
        assertThat(response.occurrences()).extracting("routineId").containsExactly(pyramid);
        assertThat(response.occurrences().getFirst().blockStructuralType()).isEqualTo(BlockStructuralType.PYRAMID);
    }

    @Test
    void returnsFoundFalseWhenNoPreviousLoadMatchesStructuralType() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Standard", RoutineStatus.FINISHED, LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 1), null, null, BlockStructuralType.STANDARD,
                BlockPurpose.MAIN_LIFT, List.of(set(10, "45", 60)));

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(
                1L, fixture.studentId(), fixture.exerciseId(), null, BlockStructuralType.PYRAMID, 3);

        assertThat(response.found()).isFalse();
        assertThat(response.occurrences()).isEmpty();
    }

    @Test
    void keepsBackwardCompatibilityWithoutStructuralType() {
        Fixture fixture = fixture();
        Long standard = createRoutine(fixture, "Standard", RoutineStatus.FINISHED, LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 1), null, null, BlockStructuralType.STANDARD,
                BlockPurpose.MAIN_LIFT, List.of(set(10, "45", 60)));

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(
                1L, fixture.studentId(), fixture.exerciseId(), null, 1);

        assertThat(response.found()).isTrue();
        assertThat(response.occurrences()).extracting("routineId").containsExactly(standard);
    }

    @Test
    void excludeRoutineIdStillWorksWithStructuralType() {
        Fixture fixture = fixture();
        Long routineId = createRoutine(fixture, "Pyramid", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1), null, null, BlockStructuralType.PYRAMID,
                BlockPurpose.MAIN_LIFT, List.of(set(12, "30", 90), set(10, "35", 90)));

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(
                1L, fixture.studentId(), fixture.exerciseId(), routineId, BlockStructuralType.PYRAMID, 3);

        assertThat(response.found()).isFalse();
        assertThat(response.occurrences()).isEmpty();
    }

    @Test
    void limitStillWorksWithStructuralType() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Pyramid 1", RoutineStatus.FINISHED, LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 1), null, null, BlockStructuralType.PYRAMID,
                BlockPurpose.MAIN_LIFT, List.of(set(12, "30", 90)));
        createRoutine(fixture, "Pyramid 2", RoutineStatus.FINISHED, LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 1), null, null, BlockStructuralType.PYRAMID,
                BlockPurpose.MAIN_LIFT, List.of(set(10, "35", 90)));
        createRoutine(fixture, "Pyramid 3", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1), null, null, BlockStructuralType.PYRAMID,
                BlockPurpose.MAIN_LIFT, List.of(set(8, "40", 90)));

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(
                1L, fixture.studentId(), fixture.exerciseId(), null, BlockStructuralType.PYRAMID, 2);

        assertThat(response.occurrences()).hasSize(2);
        assertThat(response.occurrences()).allSatisfy(occurrence ->
                assertThat(occurrence.blockStructuralType()).isEqualTo(BlockStructuralType.PYRAMID));
    }

    @Test
    void returnsSameStructuralTypeWhenAvailable() {
        Fixture fixture = fixture();
        Long standard = createRoutine(fixture, "Standard", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1), null, null, BlockStructuralType.STANDARD,
                BlockPurpose.MAIN_LIFT, List.of(set(10, "45", 60)));
        createRoutine(fixture, "Circuit", RoutineStatus.FINISHED, LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 7, 1), null, null, BlockStructuralType.CIRCUIT,
                BlockPurpose.CONDITIONING, List.of(set(12, "20", 30)));

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(
                1L, fixture.studentId(), fixture.exerciseId(), null, BlockStructuralType.STANDARD, true, 1);

        assertThat(response.found()).isTrue();
        assertThat(response.matchType()).isEqualTo(PreviousLoadsMatchType.SAME_STRUCTURAL_TYPE);
        assertThat(response.requestedStructuralType()).isEqualTo(BlockStructuralType.STANDARD);
        assertThat(response.occurrences()).extracting("routineId").containsExactly(standard);
        assertThat(response.occurrences().getFirst().blockStructuralType()).isEqualTo(BlockStructuralType.STANDARD);
    }

    @Test
    void returnsFallbackWhenNoSameStructuralTypeExists() {
        Fixture fixture = fixture();
        Long circuit = createRoutine(fixture, "Circuit", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1), null, null, BlockStructuralType.CIRCUIT,
                BlockPurpose.CONDITIONING, List.of(set(12, "20", 30)));

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(
                1L, fixture.studentId(), fixture.exerciseId(), null, BlockStructuralType.STANDARD, true, 1);

        assertThat(response.found()).isTrue();
        assertThat(response.matchType()).isEqualTo(PreviousLoadsMatchType.DIFFERENT_STRUCTURAL_TYPE);
        assertThat(response.requestedStructuralType()).isEqualTo(BlockStructuralType.STANDARD);
        assertThat(response.occurrences()).extracting("routineId").containsExactly(circuit);
        assertThat(response.occurrences().getFirst().blockStructuralType()).isEqualTo(BlockStructuralType.CIRCUIT);
    }

    @Test
    void doesNotFallbackWhenIncludeFallbackIsFalse() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Circuit", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1), null, null, BlockStructuralType.CIRCUIT,
                BlockPurpose.CONDITIONING, List.of(set(12, "20", 30)));

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(
                1L, fixture.studentId(), fixture.exerciseId(), null, BlockStructuralType.STANDARD, false, 1);

        assertThat(response.found()).isFalse();
        assertThat(response.matchType()).isEqualTo(PreviousLoadsMatchType.NONE);
        assertThat(response.requestedStructuralType()).isEqualTo(BlockStructuralType.STANDARD);
        assertThat(response.occurrences()).isEmpty();
    }

    @Test
    void returnsNoneWhenExerciseHasNoHistoryAtAll() {
        Fixture fixture = fixture();

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(
                1L, fixture.studentId(), fixture.exerciseId(), null, BlockStructuralType.STANDARD, true, 1);

        assertThat(response.found()).isFalse();
        assertThat(response.matchType()).isEqualTo(PreviousLoadsMatchType.NONE);
        assertThat(response.requestedStructuralType()).isEqualTo(BlockStructuralType.STANDARD);
        assertThat(response.occurrences()).isEmpty();
    }

    @Test
    void excludeRoutineIdAppliesToFallbackSearch() {
        Fixture fixture = fixture();
        Long circuit = createRoutine(fixture, "Circuit", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1), null, null, BlockStructuralType.CIRCUIT,
                BlockPurpose.CONDITIONING, List.of(set(12, "20", 30)));

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(
                1L, fixture.studentId(), fixture.exerciseId(), circuit, BlockStructuralType.STANDARD, true, 1);

        assertThat(response.found()).isFalse();
        assertThat(response.matchType()).isEqualTo(PreviousLoadsMatchType.NONE);
        assertThat(response.occurrences()).isEmpty();
    }

    @Test
    void limitAppliesToFallbackSearch() {
        Fixture fixture = fixture();
        Long older = createRoutine(fixture, "Circuit old", RoutineStatus.FINISHED, LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 1), null, null, BlockStructuralType.CIRCUIT,
                BlockPurpose.CONDITIONING, List.of(set(12, "20", 30)));
        Long latest = createRoutine(fixture, "Circuit latest", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1), null, null, BlockStructuralType.CIRCUIT,
                BlockPurpose.CONDITIONING, List.of(set(10, "25", 30)));

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(
                1L, fixture.studentId(), fixture.exerciseId(), null, BlockStructuralType.STANDARD, true, 1);

        assertThat(response.matchType()).isEqualTo(PreviousLoadsMatchType.DIFFERENT_STRUCTURAL_TYPE);
        assertThat(response.occurrences()).hasSize(1);
        assertThat(response.occurrences()).extracting("routineId").containsExactly(latest);
        assertThat(response.occurrences()).extracting("routineId").doesNotContain(older);
    }

    @Test
    void returnsFoundFalseWhenNoData() {
        Fixture fixture = fixture();

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(1L, fixture.studentId(), fixture.exerciseId(), null, 1);

        assertThat(response.found()).isFalse();
        assertThat(response.occurrences()).isEmpty();
    }

    @Test
    void failsWith404WhenStudentBelongsToAnotherGym() {
        Long otherGymId = otherGymId();
        Long otherStudentId = createStudent(otherGymId);
        Long exerciseId = createExercise(1L, MeasurementType.REPS_WEIGHT, "Press");

        assertThatThrownBy(() -> previousLoadsService.getPreviousLoads(1L, otherStudentId, exerciseId, null, 1))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Alumno no encontrado");
    }

    @Test
    void failsWith404WhenExerciseBelongsToAnotherGym() {
        Long otherGymId = otherGymId();
        Long studentId = createStudent(1L);
        Long otherExerciseId = createExercise(otherGymId, MeasurementType.REPS_WEIGHT, "Press externo");

        assertThatThrownBy(() -> previousLoadsService.getPreviousLoads(1L, studentId, otherExerciseId, null, 1))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Ejercicio no encontrado");
    }

    @Test
    void failsWith404WhenStudentDoesNotExist() {
        Long exerciseId = createExercise(1L, MeasurementType.REPS_WEIGHT, "Press");

        assertThatThrownBy(() -> previousLoadsService.getPreviousLoads(1L, 999_999L, exerciseId, null, 1))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Alumno no encontrado");
    }

    @Test
    void failsWith404WhenExerciseDoesNotExist() {
        Long studentId = createStudent(1L);

        assertThatThrownBy(() -> previousLoadsService.getPreviousLoads(1L, studentId, 999_999L, null, 1))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Ejercicio no encontrado");
    }

    @Test
    void doesNotExposeInternalNotes() throws Exception {
        Fixture fixture = fixture();
        createRoutine(fixture, "Rutina visible", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1), "Lesion en rodilla derecha", null);

        String json = objectMapper.writeValueAsString(previousLoadsService.getPreviousLoads(
                1L, fixture.studentId(), fixture.exerciseId(), null, 1));

        assertThat(json).doesNotContain("Lesion en rodilla derecha");
    }

    @Test
    void doesNotExposeClosureNotes() throws Exception {
        Fixture fixture = fixture();
        Long routineId = createRoutine(fixture, "Rutina visible", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1));
        routineRepository.findById(routineId).orElseThrow().setClosureNotes("Alumno se fue de viaje");

        String json = objectMapper.writeValueAsString(previousLoadsService.getPreviousLoads(
                1L, fixture.studentId(), fixture.exerciseId(), null, 1));

        assertThat(json).doesNotContain("Alumno se fue de viaje");
    }

    @Test
    void doesNotExposeStudentInjuries() throws Exception {
        Fixture fixture = fixture();
        createRoutine(fixture, "Rutina visible", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));
        addInjury(fixture.studentId(), "Rodilla", "Dolor patelar sensible");

        String json = objectMapper.writeValueAsString(previousLoadsService.getPreviousLoads(
                1L, fixture.studentId(), fixture.exerciseId(), null, 1));

        assertThat(json).doesNotContain("Rodilla", "Dolor patelar sensible");
    }

    @Test
    void doesNotExposeStudentNotes() throws Exception {
        Fixture fixture = fixture();
        createRoutine(fixture, "Rutina visible", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));
        addStudentNote(fixture.studentId(), "Dato privado del profesor");

        String json = objectMapper.writeValueAsString(previousLoadsService.getPreviousLoads(
                1L, fixture.studentId(), fixture.exerciseId(), null, BlockStructuralType.REVERSE_PYRAMID, 1));

        assertThat(json).doesNotContain("Dato privado del profesor");
    }

    @Test
    void returnsSetsOrderedBySetNumber() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Rutina sets", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1), null, null, BlockStructuralType.REVERSE_PYRAMID,
                BlockPurpose.MAIN_LIFT, List.of(
                        set(8, "70", 90),
                        set(6, "80", 90),
                        set(10, "60", 90),
                        set(12, "50", 90)));

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(1L, fixture.studentId(), fixture.exerciseId(), null, 1);

        assertThat(response.occurrences().getFirst().sets())
                .extracting("setNumber")
                .containsExactly(1, 2, 3, 4);
    }

    @Test
    void returnsCircuitExerciseMeasurementCorrectly() {
        Long studentId = createStudent(1L);
        Long exerciseId = createExercise(1L, MeasurementType.CIRCUIT_REPS, "Burpee circuito");
        Long fillerId = createExercise(1L, MeasurementType.REPS_ONLY, "Movilidad circuito");
        Fixture fixture = new Fixture(studentId, exerciseId, fillerId);
        createRoutine(fixture, "Circuito", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1), null, null, BlockStructuralType.CIRCUIT,
                BlockPurpose.CONDITIONING, List.of(set(15, null, 30)));

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(1L, fixture.studentId(), fixture.exerciseId(), null, 1);

        assertThat(response.occurrences().getFirst().measurementType()).isEqualTo(MeasurementType.CIRCUIT_REPS);
        assertThat(response.occurrences().getFirst().blockStructuralType()).isEqualTo(BlockStructuralType.CIRCUIT);
    }

    @Test
    void returnsCorrectBlockPurposeAndStructuralType() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Accesorios", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1), null, null, BlockStructuralType.REST_PAUSE,
                BlockPurpose.ACCESSORY, List.of(set(12, "30", 60)));

        PreviousLoadsResponse response = previousLoadsService.getPreviousLoads(1L, fixture.studentId(), fixture.exerciseId(), null, 1);

        assertThat(response.occurrences().getFirst().blockPurpose()).isEqualTo(BlockPurpose.ACCESSORY);
        assertThat(response.occurrences().getFirst().blockStructuralType()).isEqualTo(BlockStructuralType.REST_PAUSE);
    }

    private void createRoutines(Fixture fixture, int count) {
        for (int i = 1; i <= count; i++) {
            createRoutine(fixture, "Routine " + i, RoutineStatus.FINISHED,
                    LocalDate.of(2026, 1, i), LocalDate.of(2026, 2, i));
        }
    }

    private Fixture fixture() {
        return new Fixture(
                createStudent(1L),
                createExercise(1L, MeasurementType.REPS_WEIGHT, "Sentadilla Goblet"),
                createExercise(1L, MeasurementType.REPS_ONLY, "Movilidad general"));
    }

    private Long createStudent(Long gymId) {
        return studentService.create(gymId, new CreateStudentRequest(
                "Ana " + System.nanoTime(),
                "Garcia",
                null,
                "555",
                null,
                null,
                "Voley",
                "Potencia",
                "Intermedio",
                null,
                LocalDate.now())).id();
    }

    private Long createExercise(Long gymId, MeasurementType measurementType, String name) {
        return exerciseService.create(gymId, new CreateExerciseRequest(
                name + " " + System.nanoTime(),
                "Desc",
                null,
                measurementType,
                null,
                null,
                List.of())).id();
    }

    private Long createRoutine(Fixture fixture, String name, RoutineStatus status, LocalDate assignedDate, LocalDate finishedDate) {
        return createRoutine(fixture, name, status, assignedDate, finishedDate, null, null);
    }

    private Long createRoutine(Fixture fixture, String name, RoutineStatus status, LocalDate assignedDate, LocalDate finishedDate,
            String internalNotes, String closureNotes) {
        return createRoutine(fixture, name, status, assignedDate, finishedDate, internalNotes, closureNotes,
                BlockStructuralType.REVERSE_PYRAMID, BlockPurpose.MAIN_LIFT, List.of(set(6, "80", 90)));
    }

    private Long createRoutine(Fixture fixture, String name, RoutineStatus status, LocalDate assignedDate, LocalDate finishedDate,
            String internalNotes, String closureNotes, BlockStructuralType structuralType, BlockPurpose purpose,
            List<RoutineExerciseSetInput> targetSets) {
        Long routineId = routineService.createFromScratch(1L, 1L, new CreateRoutineFromScratchRequest(
                fixture.studentId(),
                name,
                "Potencia",
                status,
                assignedDate,
                "Notas generales",
                internalNotes,
                List.of(day(fixture.exerciseId(), fixture.fillerExerciseId(), structuralType, purpose, targetSets)))).id();
        Routine routine = routineRepository.findById(routineId).orElseThrow();
        routine.setFinishedDate(finishedDate);
        routine.setClosureNotes(closureNotes);
        return routineId;
    }

    private RoutineDayInput day(Long targetExerciseId, Long fillerExerciseId, BlockStructuralType structuralType,
            BlockPurpose targetPurpose, List<RoutineExerciseSetInput> targetSets) {
        return new RoutineDayInput(null, null, "Dia 1 - Potencia", null, List.of(
                block("Entrada en calor", BlockStructuralType.STANDARD, BlockPurpose.WARMUP, fillerExerciseId, List.of(set(10, null, 30))),
                block("Bloque objetivo", structuralType, targetPurpose, targetExerciseId, targetSets),
                block("Vuelta a la calma", BlockStructuralType.STANDARD, BlockPurpose.COOLDOWN, fillerExerciseId, List.of(set(30, null, 30)))
        ));
    }

    private RoutineBlockInput block(String title, BlockStructuralType structuralType, BlockPurpose purpose,
            Long exerciseId, List<RoutineExerciseSetInput> sets) {
        Integer duration = structuralType == BlockStructuralType.CIRCUIT ? 720 : null;
        return new RoutineBlockInput(null, title, structuralType, purpose, duration, null, "Nota de bloque", List.of(
                new RoutineExerciseInput(exerciseId, null, "Cuidar tecnica", sets)
        ));
    }

    private RoutineExerciseSetInput set(int reps, String weight, int restSeconds) {
        return new RoutineExerciseSetInput(null, SetKind.NORMAL, reps, null, null,
                weight == null ? null : new BigDecimal(weight), null, null, restSeconds, null, null, null, false);
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

    private Long otherGymId() {
        Long id = ((Number) entityManager.createNativeQuery("SELECT COALESCE(MAX(id), 0) + 1 FROM gyms")
                .getSingleResult()).longValue();
        entityManager.createNativeQuery("""
                        INSERT INTO gyms (id, name, created_at, updated_at)
                        VALUES (:id, :name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """)
                .setParameter("id", id)
                .setParameter("name", "Otro gym " + System.nanoTime())
                .executeUpdate();
        return id;
    }

    private record Fixture(Long studentId, Long exerciseId, Long fillerExerciseId) {
    }
}
