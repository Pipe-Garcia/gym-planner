package com.gymplanner.shared.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.gymplanner.exercise.ExerciseService;
import com.gymplanner.exercise.MeasurementType;
import com.gymplanner.exercise.dto.CreateExerciseRequest;
import com.gymplanner.routine.RoutineService;
import com.gymplanner.routine.RoutineStatus;
import com.gymplanner.routine.dto.CreateRoutineFromScratchRequest;
import com.gymplanner.routine.dto.FinishRoutineRequest;
import com.gymplanner.routine.dto.RoutineBlockInput;
import com.gymplanner.routine.dto.RoutineDayInput;
import com.gymplanner.routine.dto.RoutineExerciseInput;
import com.gymplanner.routine.dto.RoutineExerciseSetInput;
import com.gymplanner.routine.dto.RoutineResponse;
import com.gymplanner.routine.dto.UpdateRoutineRequest;
import com.gymplanner.shared.blocks.BlockPurpose;
import com.gymplanner.shared.blocks.BlockStructuralType;
import com.gymplanner.shared.blocks.SetKind;
import com.gymplanner.student.StudentService;
import com.gymplanner.student.dto.CreateStudentRequest;
import com.gymplanner.student.injury.InjurySeverity;
import com.gymplanner.student.injury.StudentInjuryService;
import com.gymplanner.student.injury.dto.CreateInjuryRequest;
import com.gymplanner.student.injury.dto.InjuryResponse;
import com.gymplanner.student.injury.dto.UpdateInjuryRequest;
import com.gymplanner.student.note.StudentNoteService;
import com.gymplanner.student.note.dto.CreateNoteRequest;
import com.gymplanner.student.note.dto.NoteResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Covers logs written by application code under com.gymplanner. Hibernate SQL/bind logging
 * is intentionally out of scope here because production configuration was hardened in F2-1.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SensitiveDataLoggingTest {

    private static final Long GYM_ID = 1L;
    private static final Long USER_ID = 1L;

    @Autowired
    private RoutineService routineService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private StudentInjuryService injuryService;

    @Autowired
    private StudentNoteService noteService;

    private Logger applicationLogger;
    private Level originalLevel;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void captureApplicationLogs() {
        applicationLogger = (Logger) LoggerFactory.getLogger("com.gymplanner");
        originalLevel = applicationLogger.getLevel();
        applicationLogger.setLevel(Level.TRACE);

        appender = new ListAppender<>();
        appender.setContext(applicationLogger.getLoggerContext());
        appender.start();
        applicationLogger.addAppender(appender);
    }

    @AfterEach
    void restoreApplicationLogger() {
        applicationLogger.detachAppender(appender);
        appender.stop();
        applicationLogger.setLevel(originalLevel);
    }

    @Test
    void routineInternalNotes_areNotLogged() {
        String sentinel = "S1_SENTINEL_2a7c91";
        RoutineFixture fixture = createRoutine("create-" + sentinel);

        RoutineResponse updatedRoutine = routineService.update(
                GYM_ID,
                fixture.routineId(),
                new UpdateRoutineRequest(
                        "Rutina actualizada",
                        "Fuerza",
                        RoutineStatus.ACTIVE,
                        LocalDate.now(),
                        "Notas generales permitidas",
                        "update-" + sentinel,
                        routineDays(fixture.exerciseId())));

        assertThat(updatedRoutine.internalNotes()).contains(sentinel);
        assertSensitiveValueWasNotLogged(sentinel);
    }

    @Test
    void routineClosureNotes_areNotLogged() {
        String sentinel = "S2_SENTINEL_84bd13";
        RoutineFixture fixture = createRoutine(null);

        RoutineResponse finishedRoutine = routineService.finishRoutine(
                GYM_ID,
                USER_ID,
                fixture.routineId(),
                new FinishRoutineRequest(sentinel));

        assertThat(finishedRoutine.closureNotes()).contains(sentinel);
        assertThat(finishedRoutine.status()).isEqualTo(RoutineStatus.FINISHED);
        assertSensitiveValueWasNotLogged(sentinel);
    }

    @Test
    void studentInjuryFields_areNotLogged() {
        String sentinel = "S3_SENTINEL_5e1f77";
        Long studentId = createStudent("Injury");
        InjuryResponse injury = injuryService.create(
                GYM_ID,
                studentId,
                new CreateInjuryRequest(
                        "Rodilla " + sentinel,
                        "Dolor al flexionar " + sentinel,
                        InjurySeverity.LEVE,
                        LocalDate.now(),
                        "Controlar carga " + sentinel));

        InjuryResponse updatedInjury = injuryService.update(
                GYM_ID,
                studentId,
                injury.id(),
                new UpdateInjuryRequest(
                        "Hombro " + sentinel,
                        "Dolor actualizado " + sentinel,
                        InjurySeverity.MODERADA,
                        LocalDate.now(),
                        null,
                        true,
                        "Seguimiento " + sentinel));

        assertThat(injury.description()).contains(sentinel);
        assertThat(updatedInjury.description()).contains(sentinel);
        assertSensitiveValueWasNotLogged(sentinel);
    }

    @Test
    void studentNoteContent_isNotLogged() {
        String sentinel = "S4_SENTINEL_f093bc";
        Long studentId = createStudent("Note");

        NoteResponse note = noteService.create(
                GYM_ID,
                studentId,
                USER_ID,
                new CreateNoteRequest("Contenido privado " + sentinel));

        assertThat(note.content()).contains(sentinel);
        assertSensitiveValueWasNotLogged(sentinel);
    }

    private RoutineFixture createRoutine(String internalNotes) {
        Long studentId = createStudent("Routine");
        Long exerciseId = exerciseService.create(
                GYM_ID,
                new CreateExerciseRequest(
                        "Ejercicio logging " + System.nanoTime(),
                        "Descripción de ejercicio",
                        null,
                        MeasurementType.REPS_WEIGHT,
                        null,
                        null,
                        List.of()))
                .id();
        Long routineId = routineService.createFromScratch(
                GYM_ID,
                USER_ID,
                new CreateRoutineFromScratchRequest(
                        studentId,
                        "Rutina logging",
                        "Fuerza",
                        RoutineStatus.ACTIVE,
                        LocalDate.now(),
                        "Notas generales permitidas",
                        internalNotes,
                        routineDays(exerciseId)))
                .id();
        return new RoutineFixture(exerciseId, routineId);
    }

    private Long createStudent(String label) {
        return studentService.create(
                GYM_ID,
                new CreateStudentRequest(
                        "Privacy",
                        label,
                        null,
                        null,
                        null,
                        LocalDate.of(2000, 1, 1),
                        "Fuerza",
                        "Privacidad",
                        "Intermedio",
                        null,
                        LocalDate.now()))
                .id();
    }

    private List<RoutineDayInput> routineDays(Long exerciseId) {
        return List.of(new RoutineDayInput(
                null,
                null,
                "Día 1",
                null,
                List.of(
                        routineBlock("Calentamiento", BlockPurpose.WARMUP, exerciseId),
                        routineBlock("Parte principal", BlockPurpose.MAIN_LIFT, exerciseId),
                        routineBlock("Vuelta a la calma", BlockPurpose.COOLDOWN, exerciseId))));
    }

    private RoutineBlockInput routineBlock(String title, BlockPurpose purpose, Long exerciseId) {
        return new RoutineBlockInput(
                null,
                title,
                BlockStructuralType.STANDARD,
                purpose,
                null,
                null,
                null,
                null,
                List.of(new RoutineExerciseInput(
                        exerciseId,
                        null,
                        null,
                        List.of(new RoutineExerciseSetInput(
                                null,
                                SetKind.NORMAL,
                                10,
                                null,
                                null,
                                null,
                                null,
                                null,
                                60,
                                null,
                                null,
                                null,
                                null,
                                false)))));
    }

    private void assertSensitiveValueWasNotLogged(String sentinel) {
        String capturedLogs = appender.list.stream()
                .map(event -> {
                    String throwableMessage = event.getThrowableProxy() == null
                            ? ""
                            : String.valueOf(event.getThrowableProxy().getMessage());
                    return event.getFormattedMessage() + System.lineSeparator() + throwableMessage;
                })
                .collect(Collectors.joining(System.lineSeparator()));

        assertThat(capturedLogs).doesNotContain(sentinel);
    }

    private record RoutineFixture(Long exerciseId, Long routineId) {
    }
}
