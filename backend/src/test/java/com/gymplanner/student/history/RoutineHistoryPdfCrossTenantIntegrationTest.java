package com.gymplanner.student.history;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymplanner.exercise.ExerciseService;
import com.gymplanner.exercise.MeasurementType;
import com.gymplanner.exercise.dto.CreateExerciseRequest;
import com.gymplanner.exercise.dto.ExerciseResponse;
import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymRepository;
import com.gymplanner.pdf.RoutinePdfService;
import com.gymplanner.pdf.WhatsAppTextService;
import com.gymplanner.routine.RoutineService;
import com.gymplanner.routine.RoutineStatus;
import com.gymplanner.routine.dto.CreateRoutineFromScratchRequest;
import com.gymplanner.routine.dto.RoutineBlockInput;
import com.gymplanner.routine.dto.RoutineDayInput;
import com.gymplanner.routine.dto.RoutineExerciseInput;
import com.gymplanner.routine.dto.RoutineExerciseSetInput;
import com.gymplanner.routine.dto.RoutineResponse;
import com.gymplanner.shared.blocks.BlockPurpose;
import com.gymplanner.shared.blocks.BlockStructuralType;
import com.gymplanner.shared.blocks.SetKind;
import com.gymplanner.shared.exception.NotFoundException;
import com.gymplanner.student.StudentService;
import com.gymplanner.student.dto.CreateStudentRequest;
import com.gymplanner.student.dto.StudentResponse;
import com.gymplanner.support.PostgresIntegrationTest;
import com.gymplanner.user.User;
import com.gymplanner.user.UserRepository;
import com.gymplanner.user.UserRole;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class RoutineHistoryPdfCrossTenantIntegrationTest extends PostgresIntegrationTest {

    private static final Long GYM_A_ID = 1L;
    private static final String ROUTINE_NOT_FOUND_MESSAGE = "Rutina no encontrada";

    @Autowired
    private RoutinePdfService routinePdfService;

    @Autowired
    private WhatsAppTextService whatsAppTextService;

    @Autowired
    private StudentHistoryService studentHistoryService;

    @Autowired
    private PreviousLoadsService previousLoadsService;

    @Autowired
    private RoutineService routineService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private GymRepository gymRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void generatePdfDoesNotExposeRoutineFromAnotherGym() {
        ActiveRoutineFixture fixture = createOtherGymActiveRoutine("Generate PDF");

        assertRoutineNotFound(() -> routinePdfService.generatePdf(fixture.routine().id(), GYM_A_ID));
    }

    @Test
    void buildDtoDoesNotExposeRoutineFromAnotherGym() {
        ActiveRoutineFixture fixture = createOtherGymActiveRoutine("Build DTO");

        assertRoutineNotFound(() -> routinePdfService.buildDto(fixture.routine().id(), GYM_A_ID));
    }

    @Test
    void whatsAppTextDoesNotExposeRoutineFromAnotherGym() {
        ActiveRoutineFixture fixture = createOtherGymActiveRoutine("WhatsApp");

        assertRoutineNotFound(() -> whatsAppTextService.generateText(fixture.routine().id(), GYM_A_ID));
    }

    @Test
    void summaryDoesNotExposeHistoryForStudentFromAnotherGym() {
        ActiveRoutineFixture fixture = createOtherGymActiveRoutine("Summary");

        assertNotFound(() -> studentHistoryService.getSummary(GYM_A_ID, fixture.student().id()));
    }

    @Test
    void timelineDoesNotExposeHistoryForStudentFromAnotherGym() {
        ActiveRoutineFixture fixture = createOtherGymActiveRoutine("Timeline");

        assertNotFound(() -> studentHistoryService.getTimeline(GYM_A_ID, fixture.student().id(), 0, 20));
    }

    @Test
    void exerciseHistoryDoesNotExposeStudentFromAnotherGym() {
        ActiveRoutineFixture fixture = createOtherGymActiveRoutine("Exercise History");

        assertNotFound(() -> studentHistoryService.getExerciseHistory(
                GYM_A_ID, fixture.student().id(), null, 0, 20));
    }

    @Test
    void exerciseOccurrencesDoNotExposeStudentFromAnotherGym() {
        ActiveRoutineFixture fixture = createOtherGymActiveRoutine("Occurrences");

        assertNotFound(() -> studentHistoryService.getExerciseOccurrences(
                GYM_A_ID,
                fixture.student().id(),
                fixture.exercise().id(),
                0,
                20));
    }

    @Test
    void previousLoadsDoNotExposeStudentFromAnotherGym() {
        ActiveRoutineFixture fixture = createOtherGymActiveRoutine("Previous Loads");

        assertNotFound(() -> previousLoadsService.getPreviousLoads(
                GYM_A_ID,
                fixture.student().id(),
                fixture.exercise().id(),
                null,
                10));
    }

    private ActiveRoutineFixture createOtherGymActiveRoutine(String label) {
        String suffix = uniqueSuffix();
        Gym gym = createOtherGym("Other Gym History " + label + " " + suffix);
        User user = createUser(gym, "history-" + suffix + "@gymplanner.local");
        StudentResponse student = createStudent(
                gym.getId(), "Other", "History" + label.replace(" ", ""), "b-history-" + suffix);
        ExerciseResponse exercise = createExercise(
                gym.getId(), "History " + label + " Exercise " + suffix);
        RoutineResponse routine = routineService.createFromScratch(
                gym.getId(),
                user.getId(),
                activeRequest(student.id(), "History " + label + " Routine " + suffix, exercise.id()));
        return new ActiveRoutineFixture(gym, user, student, exercise, routine);
    }

    private Gym createOtherGym(String name) {
        Gym gym = new Gym();
        gym.setName(name);
        return gymRepository.save(gym);
    }

    private User createUser(Gym gym, String email) {
        User user = new User();
        user.setGym(gym);
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setFullName("Other Owner");
        user.setRole(UserRole.OWNER);
        user.setActive(true);
        return userRepository.save(user);
    }

    private StudentResponse createStudent(Long gymId, String firstName, String lastName, String documentId) {
        return studentService.create(
                gymId,
                new CreateStudentRequest(
                        firstName,
                        lastName,
                        documentId,
                        null,
                        null,
                        LocalDate.of(2000, 1, 1),
                        "Futbol",
                        "Fuerza",
                        "Intermedio",
                        null,
                        LocalDate.now()));
    }

    private ExerciseResponse createExercise(Long gymId, String name) {
        return exerciseService.create(
                gymId,
                new CreateExerciseRequest(
                        name,
                        null,
                        null,
                        MeasurementType.REPS_WEIGHT,
                        null,
                        null,
                        null));
    }

    private CreateRoutineFromScratchRequest activeRequest(Long studentId, String name, Long exerciseId) {
        return new CreateRoutineFromScratchRequest(
                studentId,
                name,
                "Fuerza",
                RoutineStatus.ACTIVE,
                LocalDate.now(),
                null,
                null,
                List.of(new RoutineDayInput(
                        null,
                        null,
                        "Dia 1",
                        null,
                        List.of(
                                block("Warmup", BlockPurpose.WARMUP, exerciseId),
                                block("Main", BlockPurpose.MAIN_LIFT, exerciseId),
                                block("Cooldown", BlockPurpose.COOLDOWN, exerciseId)))));
    }

    private RoutineBlockInput block(String title, BlockPurpose purpose, Long exerciseId) {
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

    private void assertRoutineNotFound(ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isExactlyInstanceOf(NotFoundException.class)
                .hasMessage(ROUTINE_NOT_FOUND_MESSAGE);
    }

    private void assertNotFound(ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isExactlyInstanceOf(NotFoundException.class);
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private record ActiveRoutineFixture(
            Gym gym,
            User user,
            StudentResponse student,
            ExerciseResponse exercise,
            RoutineResponse routine) {}
}
