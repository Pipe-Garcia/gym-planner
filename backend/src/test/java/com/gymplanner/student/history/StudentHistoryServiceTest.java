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
import com.gymplanner.shared.pagination.PageResponse;
import com.gymplanner.student.Student;
import com.gymplanner.student.StudentRepository;
import com.gymplanner.student.StudentService;
import com.gymplanner.student.dto.CreateStudentRequest;
import com.gymplanner.student.history.dto.StudentExerciseHistoryItemResponse;
import com.gymplanner.student.history.dto.StudentExerciseOccurrenceResponse;
import com.gymplanner.student.history.dto.StudentHistorySummaryResponse;
import com.gymplanner.student.history.dto.StudentRoutineTimelineItemResponse;
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
class StudentHistoryServiceTest {
    @Autowired StudentHistoryService studentHistoryService;
    @Autowired StudentService studentService;
    @Autowired ExerciseService exerciseService;
    @Autowired RoutineService routineService;
    @Autowired RoutineRepository routineRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired StudentInjuryRepository injuryRepository;
    @Autowired StudentNoteRepository noteRepository;
    @Autowired UserRepository userRepository;
    @Autowired EntityManager entityManager;
    @Autowired ObjectMapper objectMapper;

    @Test
    void summaryCountsOnlyNonDraftRoutines() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Draft", RoutineStatus.DRAFT, LocalDate.of(2026, 1, 1), null);
        createRoutine(fixture, "Active", RoutineStatus.ACTIVE, LocalDate.of(2026, 2, 1), null);
        createRoutine(fixture, "Finished", RoutineStatus.FINISHED, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1));

        assertThat(studentHistoryService.getSummary(1L, fixture.studentId()).totalRoutines()).isEqualTo(2);
    }

    @Test
    void summaryReturnsActiveRoutine() {
        Fixture fixture = fixture();
        Long activeId = createRoutine(fixture, "Rutina activa", RoutineStatus.ACTIVE, LocalDate.of(2026, 5, 13), null);

        StudentHistorySummaryResponse response = studentHistoryService.getSummary(1L, fixture.studentId());

        assertThat(response.activeRoutineId()).isEqualTo(activeId);
        assertThat(response.activeRoutineName()).isEqualTo("Rutina activa");
        assertThat(response.activeRoutineAssignedDate()).isEqualTo(LocalDate.of(2026, 5, 13));
    }

    @Test
    void summaryReturnsDistinctExerciseCount() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Uno", RoutineStatus.FINISHED, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));

        assertThat(studentHistoryService.getSummary(1L, fixture.studentId()).distinctExercisesCount()).isEqualTo(2);
    }

    @Test
    void summaryReturnsTrainingSinceFromFirstNonDraftRoutine() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Draft", RoutineStatus.DRAFT, LocalDate.of(2026, 1, 1), null);
        createRoutine(fixture, "First", RoutineStatus.FINISHED, LocalDate.of(2026, 3, 5), LocalDate.of(2026, 4, 1));
        createRoutine(fixture, "Second", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        assertThat(studentHistoryService.getSummary(1L, fixture.studentId()).trainingSince()).isEqualTo(LocalDate.of(2026, 3, 5));
    }

    @Test
    void summaryForStudentWithoutRoutinesReturnsZerosAndNulls() {
        Long studentId = createStudent(1L);

        StudentHistorySummaryResponse response = studentHistoryService.getSummary(1L, studentId);

        assertThat(response.totalRoutines()).isZero();
        assertThat(response.activeRoutineId()).isNull();
        assertThat(response.activeRoutineDaysCount()).isNull();
        assertThat(response.distinctExercisesCount()).isZero();
        assertThat(response.trainingSince()).isNull();
    }

    @Test
    void timelineExcludesDraftRoutines() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Draft", RoutineStatus.DRAFT, LocalDate.of(2026, 5, 1), null);
        createRoutine(fixture, "Visible", RoutineStatus.FINISHED, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1));

        PageResponse<StudentRoutineTimelineItemResponse> response = studentHistoryService.getTimeline(1L, fixture.studentId(), 0, 10);

        assertThat(response.content()).extracting(StudentRoutineTimelineItemResponse::routineName).containsExactly("Visible");
    }

    @Test
    void timelineOrdersByAssignedDateDesc() {
        Fixture fixture = fixture();
        Long older = createRoutine(fixture, "Older", RoutineStatus.FINISHED, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));
        Long newer = createRoutine(fixture, "Newer", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        assertThat(studentHistoryService.getTimeline(1L, fixture.studentId(), 0, 10).content())
                .extracting(StudentRoutineTimelineItemResponse::routineId)
                .containsExactly(newer, older);
    }

    @Test
    void timelineIncludesCounts() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Counts", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        StudentRoutineTimelineItemResponse item = studentHistoryService.getTimeline(1L, fixture.studentId(), 0, 10).content().getFirst();

        assertThat(item.daysCount()).isEqualTo(1);
        assertThat(item.blocksCount()).isEqualTo(3);
        assertThat(item.exercisesCount()).isEqualTo(3);
    }

    @Test
    void timelineIncludesClosureNotesButNotInternalNotes() throws Exception {
        Fixture fixture = fixture();
        createRoutine(fixture, "Privacidad", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1),
                "nota interna sensible", "cierre visible");

        String json = objectMapper.writeValueAsString(studentHistoryService.getTimeline(1L, fixture.studentId(), 0, 10));

        assertThat(json).contains("cierre visible");
        assertThat(json).doesNotContain("nota interna sensible");
    }

    @Test
    void timelineIsGymScoped() {
        Long otherStudentId = createStudent(otherGymId());

        assertThatThrownBy(() -> studentHistoryService.getTimeline(1L, otherStudentId, 0, 10))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Alumno no encontrado");
    }

    @Test
    void timelinePaginates() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Uno", RoutineStatus.FINISHED, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));
        createRoutine(fixture, "Dos", RoutineStatus.FINISHED, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1));
        createRoutine(fixture, "Tres", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        PageResponse<StudentRoutineTimelineItemResponse> response = studentHistoryService.getTimeline(1L, fixture.studentId(), 1, 2);

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(2);
    }

    @Test
    void exerciseHistoryWorksWhenSearchIsOmitted() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Historial", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        assertThat(studentHistoryService.getExerciseHistory(1L, fixture.studentId(), null, 0, 20).content())
                .extracting(StudentExerciseHistoryItemResponse::exerciseId)
                .containsExactlyInAnyOrder(fixture.exerciseId(), fixture.fillerExerciseId());
    }

    @Test
    void exerciseHistoryWorksWhenSearchIsEmptyString() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Historial", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        assertThat(studentHistoryService.getExerciseHistory(1L, fixture.studentId(), "", 0, 20).content())
                .extracting(StudentExerciseHistoryItemResponse::exerciseId)
                .containsExactlyInAnyOrder(fixture.exerciseId(), fixture.fillerExerciseId());
    }

    @Test
    void exerciseHistoryWorksWhenSearchIsBlank() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Historial", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        assertThat(studentHistoryService.getExerciseHistory(1L, fixture.studentId(), "   ", 0, 20).content())
                .extracting(StudentExerciseHistoryItemResponse::exerciseId)
                .containsExactlyInAnyOrder(fixture.exerciseId(), fixture.fillerExerciseId());
    }

    @Test
    void exerciseHistorySearchesCaseInsensitive() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Historial", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        assertThat(studentHistoryService.getExerciseHistory(1L, fixture.studentId(), "SENTADILLA", 0, 20).content())
                .singleElement()
                .extracting(StudentExerciseHistoryItemResponse::exerciseId)
                .isEqualTo(fixture.exerciseId());
    }

    @Test
    void exerciseHistoryReturnsEmptyPageWhenNoExercises() {
        Long studentId = createStudent(1L);

        PageResponse<StudentExerciseHistoryItemResponse> response = studentHistoryService.getExerciseHistory(1L, studentId, null, 0, 20);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }

    @Test
    void exerciseHistoryDoesNotReturn500WithMultipleStructuralTypes() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Finished standard", RoutineStatus.FINISHED, LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 1), BlockStructuralType.STANDARD);
        createRoutine(fixture, "Active pyramid", RoutineStatus.ACTIVE, LocalDate.of(2026, 6, 1),
                null, BlockStructuralType.PYRAMID);

        StudentExerciseHistoryItemResponse item = findExerciseHistory(fixture, fixture.exerciseId());

        assertThat(item.timesUsed()).isEqualTo(2);
        assertThat(item.structuralTypesUsed()).containsExactly(BlockStructuralType.PYRAMID, BlockStructuralType.STANDARD);
        assertThat(item.lastStructuralType()).isEqualTo(BlockStructuralType.PYRAMID);
        assertThat(item.lastRoutineName()).isEqualTo("Active pyramid");
    }

    @Test
    void exerciseHistoryOrdersByLastPerformedDateDesc() {
        Long studentId = createStudent(1L);
        Long olderExerciseId = createExercise(1L, MeasurementType.REPS_WEIGHT, "Press");
        Long newerExerciseId = createExercise(1L, MeasurementType.REPS_WEIGHT, "Remo");
        createRoutine(new Fixture(studentId, olderExerciseId, olderExerciseId), "Older", RoutineStatus.FINISHED, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), BlockStructuralType.STANDARD);
        createRoutine(new Fixture(studentId, newerExerciseId, newerExerciseId), "Newer", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1), BlockStructuralType.STANDARD);

        assertThat(studentHistoryService.getExerciseHistory(1L, studentId, null, 0, 20).content())
                .extracting(StudentExerciseHistoryItemResponse::exerciseId)
                .containsExactly(newerExerciseId, olderExerciseId);
    }

    @Test
    void exerciseHistoryCountsTimesUsed() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Uno", RoutineStatus.FINISHED, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));
        createRoutine(fixture, "Dos", RoutineStatus.FINISHED, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1));

        StudentExerciseHistoryItemResponse item = findExerciseHistory(fixture, fixture.exerciseId());

        assertThat(item.timesUsed()).isEqualTo(2);
    }

    @Test
    void exerciseHistoryIncludesStructuralTypesUsed() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Standard", RoutineStatus.FINISHED, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), BlockStructuralType.STANDARD);
        createRoutine(fixture, "Pyramid", RoutineStatus.FINISHED, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1), BlockStructuralType.PYRAMID);

        StudentExerciseHistoryItemResponse item = findExerciseHistory(fixture, fixture.exerciseId());

        assertThat(item.structuralTypesUsed()).containsExactly(BlockStructuralType.PYRAMID, BlockStructuralType.STANDARD);
        assertThat(item.lastStructuralType()).isEqualTo(BlockStructuralType.PYRAMID);
    }

    @Test
    void exerciseHistoryExcludesDraftRoutines() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Draft", RoutineStatus.DRAFT, LocalDate.of(2026, 5, 1), null);

        assertThat(studentHistoryService.getExerciseHistory(1L, fixture.studentId(), null, 0, 20).content()).isEmpty();
    }

    @Test
    void exerciseHistoryIsGymScoped() {
        Long otherStudentId = createStudent(otherGymId());

        assertThatThrownBy(() -> studentHistoryService.getExerciseHistory(1L, otherStudentId, null, 0, 20))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Alumno no encontrado");
    }

    @Test
    void occurrencesReturnAllAppearancesForExercise() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Uno", RoutineStatus.FINISHED, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));
        createRoutine(fixture, "Dos", RoutineStatus.FINISHED, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1));

        assertThat(studentHistoryService.getExerciseOccurrences(1L, fixture.studentId(), fixture.exerciseId(), 0, 10).content()).hasSize(2);
    }

    @Test
    void occurrencesIncludeSets() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Sets", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        StudentExerciseOccurrenceResponse occurrence = studentHistoryService.getExerciseOccurrences(1L, fixture.studentId(), fixture.exerciseId(), 0, 10).content().getFirst();

        assertThat(occurrence.sets()).hasSize(1);
        assertThat(occurrence.sets().getFirst().targetWeightKg()).isEqualByComparingTo("80");
        assertThat(occurrence.sets().getFirst().notes()).isEqualTo("set notes");
    }

    @Test
    void occurrencesIncludeExecutionCueWhenPresent() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Sets con indicacion", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1), null, null, BlockStructuralType.REVERSE_PYRAMID, "recorrido completo");

        StudentExerciseOccurrenceResponse occurrence = studentHistoryService.getExerciseOccurrences(1L, fixture.studentId(), fixture.exerciseId(), 0, 10).content().getFirst();

        assertThat(occurrence.sets().getFirst().executionCue()).isEqualTo("recorrido completo");
    }

    @Test
    void occurrencesReturnNullExecutionCueWhenAbsent() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Sets sin indicacion", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        StudentExerciseOccurrenceResponse occurrence = studentHistoryService.getExerciseOccurrences(1L, fixture.studentId(), fixture.exerciseId(), 0, 10).content().getFirst();

        assertThat(occurrence.sets().getFirst().executionCue()).isNull();
    }

    @Test
    void occurrencesOrderByEffectiveDateDesc() {
        Fixture fixture = fixture();
        Long older = createRoutine(fixture, "Older", RoutineStatus.FINISHED, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));
        Long newer = createRoutine(fixture, "Newer", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        assertThat(studentHistoryService.getExerciseOccurrences(1L, fixture.studentId(), fixture.exerciseId(), 0, 10).content())
                .extracting(StudentExerciseOccurrenceResponse::routineId)
                .containsExactly(newer, older);
    }

    @Test
    void occurrencesExcludeDraftRoutines() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Draft", RoutineStatus.DRAFT, LocalDate.of(2026, 5, 1), null);

        assertThat(studentHistoryService.getExerciseOccurrences(1L, fixture.studentId(), fixture.exerciseId(), 0, 10).content()).isEmpty();
    }

    @Test
    void occurrencesDoNotExposePrivateData() throws Exception {
        Fixture fixture = fixture();
        createRoutine(fixture, "Privada", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1),
                "internal secreto", "closure secreto");
        addInjury(fixture.studentId(), "Rodilla", "dato medico");
        addStudentNote(fixture.studentId(), "nota interna alumno");

        String json = objectMapper.writeValueAsString(studentHistoryService.getExerciseOccurrences(1L, fixture.studentId(), fixture.exerciseId(), 0, 10));

        assertThat(json).doesNotContain("internal secreto", "closure secreto", "Rodilla", "dato medico", "nota interna alumno");
    }

    @Test
    void occurrencesPaginate() {
        Fixture fixture = fixture();
        createRoutine(fixture, "Uno", RoutineStatus.FINISHED, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));
        createRoutine(fixture, "Dos", RoutineStatus.FINISHED, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1));
        createRoutine(fixture, "Tres", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        PageResponse<StudentExerciseOccurrenceResponse> response = studentHistoryService.getExerciseOccurrences(1L, fixture.studentId(), fixture.exerciseId(), 1, 2);

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(2);
    }

    @Test
    void occurrencesReturnEmptyPageWhenNoHistory() {
        Fixture fixture = fixture();

        PageResponse<StudentExerciseOccurrenceResponse> response = studentHistoryService.getExerciseOccurrences(1L, fixture.studentId(), fixture.exerciseId(), 0, 10);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }

    private StudentExerciseHistoryItemResponse findExerciseHistory(Fixture fixture, Long exerciseId) {
        return studentHistoryService.getExerciseHistory(1L, fixture.studentId(), null, 0, 20).content().stream()
                .filter(item -> item.exerciseId().equals(exerciseId))
                .findFirst()
                .orElseThrow();
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
        return createRoutine(fixture, name, status, assignedDate, finishedDate, null, null, BlockStructuralType.REVERSE_PYRAMID);
    }

    private Long createRoutine(Fixture fixture, String name, RoutineStatus status, LocalDate assignedDate, LocalDate finishedDate,
            BlockStructuralType structuralType) {
        return createRoutine(fixture, name, status, assignedDate, finishedDate, null, null, structuralType);
    }

    private Long createRoutine(Fixture fixture, String name, RoutineStatus status, LocalDate assignedDate, LocalDate finishedDate,
            String internalNotes, String closureNotes) {
        return createRoutine(fixture, name, status, assignedDate, finishedDate, internalNotes, closureNotes, BlockStructuralType.REVERSE_PYRAMID);
    }

    private Long createRoutine(Fixture fixture, String name, RoutineStatus status, LocalDate assignedDate, LocalDate finishedDate,
            String internalNotes, String closureNotes, BlockStructuralType structuralType) {
        return createRoutine(fixture, name, status, assignedDate, finishedDate, internalNotes, closureNotes, structuralType, null);
    }

    private Long createRoutine(Fixture fixture, String name, RoutineStatus status, LocalDate assignedDate, LocalDate finishedDate,
            String internalNotes, String closureNotes, BlockStructuralType structuralType, String executionCue) {
        Long routineId = routineService.createFromScratch(1L, 1L, new CreateRoutineFromScratchRequest(
                fixture.studentId(),
                name,
                "Potencia",
                status,
                assignedDate,
                "Notas generales",
                internalNotes,
                List.of(day(fixture.exerciseId(), fixture.fillerExerciseId(), structuralType, executionCue)))).id();
        Routine routine = routineRepository.findById(routineId).orElseThrow();
        routine.setFinishedDate(finishedDate);
        routine.setClosureNotes(closureNotes);
        return routineId;
    }

    private RoutineDayInput day(Long targetExerciseId, Long fillerExerciseId, BlockStructuralType structuralType) {
        return day(targetExerciseId, fillerExerciseId, structuralType, null);
    }

    private RoutineDayInput day(Long targetExerciseId, Long fillerExerciseId, BlockStructuralType structuralType, String executionCue) {
        return new RoutineDayInput(null, null, "Dia 1", null, List.of(
                block("Entrada en calor", BlockStructuralType.STANDARD, BlockPurpose.WARMUP, fillerExerciseId, null, null),
                block("Bloque objetivo", structuralType, BlockPurpose.MAIN_LIFT, targetExerciseId, "Cuidar tecnica", "80", executionCue),
                block("Movilidad", BlockStructuralType.STANDARD, BlockPurpose.COOLDOWN, fillerExerciseId, null, null)
        ));
    }

    private RoutineBlockInput block(String title, BlockStructuralType structuralType, BlockPurpose purpose, Long exerciseId,
            String exerciseNotes, String weight) {
        return block(title, structuralType, purpose, exerciseId, exerciseNotes, weight, null);
    }

    private RoutineBlockInput block(String title, BlockStructuralType structuralType, BlockPurpose purpose, Long exerciseId,
            String exerciseNotes, String weight, String executionCue) {
        return new RoutineBlockInput(null, title, structuralType, purpose, null, null, "Nota de bloque", List.of(
                new RoutineExerciseInput(exerciseId, null, exerciseNotes, List.of(set(6, weight, executionCue)))
        ));
    }

    private RoutineExerciseSetInput set(int reps, String weight) {
        return set(reps, weight, null);
    }

    private RoutineExerciseSetInput set(int reps, String weight, String executionCue) {
        return new RoutineExerciseSetInput(null, SetKind.NORMAL, reps, null, null,
                weight == null ? null : new BigDecimal(weight), null, null, 60, "2-0-2", executionCue, 8, "set notes", false);
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
