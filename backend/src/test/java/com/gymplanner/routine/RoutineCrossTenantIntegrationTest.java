package com.gymplanner.routine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymplanner.exercise.ExerciseService;
import com.gymplanner.exercise.MeasurementType;
import com.gymplanner.exercise.dto.CreateExerciseRequest;
import com.gymplanner.exercise.dto.ExerciseResponse;
import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymRepository;
import com.gymplanner.routine.dto.CreateNextRoutineRequest;
import com.gymplanner.routine.dto.CreateRoutineFromScratchRequest;
import com.gymplanner.routine.dto.CreateRoutineFromTemplateRequest;
import com.gymplanner.routine.dto.DuplicateRoutineRequest;
import com.gymplanner.routine.dto.FinishAndCreateNextRequest;
import com.gymplanner.routine.dto.FinishRoutineRequest;
import com.gymplanner.routine.dto.RoutineBlockInput;
import com.gymplanner.routine.dto.RoutineDayInput;
import com.gymplanner.routine.dto.RoutineExerciseInput;
import com.gymplanner.routine.dto.RoutineExerciseSetInput;
import com.gymplanner.routine.dto.RoutineResponse;
import com.gymplanner.routine.dto.RoutineSummaryResponse;
import com.gymplanner.routine.dto.UpdateRoutineRequest;
import com.gymplanner.shared.blocks.BlockPurpose;
import com.gymplanner.shared.blocks.BlockStructuralType;
import com.gymplanner.shared.blocks.SetKind;
import com.gymplanner.shared.exception.NotFoundException;
import com.gymplanner.student.StudentService;
import com.gymplanner.student.dto.CreateStudentRequest;
import com.gymplanner.student.dto.StudentResponse;
import com.gymplanner.support.PostgresIntegrationTest;
import com.gymplanner.template.TemplateService;
import com.gymplanner.template.dto.CreateTemplateRequest;
import com.gymplanner.template.dto.TemplateBlockInput;
import com.gymplanner.template.dto.TemplateDayInput;
import com.gymplanner.template.dto.TemplateExerciseInput;
import com.gymplanner.template.dto.TemplateExerciseSetInput;
import com.gymplanner.template.dto.TemplateResponse;
import com.gymplanner.user.User;
import com.gymplanner.user.UserRepository;
import com.gymplanner.user.UserRole;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class RoutineCrossTenantIntegrationTest extends PostgresIntegrationTest {

    private static final Long GYM_A_ID = 1L;
    private static final Long USER_A_ID = 1L;

    @Autowired
    private RoutineService routineService;

    @Autowired
    private RoutineFromTemplateService routineFromTemplateService;

    @Autowired
    private RoutineLifecycleService routineLifecycleService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private GymRepository gymRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void getDoesNotExposeRoutineFromAnotherGym() {
        OtherGymFixture fixture = createOtherGymDraft("Get");

        assertNotFound(() -> routineService.get(GYM_A_ID, fixture.routine().id()));
    }

    @Test
    void getFullDoesNotExposeRoutineFromAnotherGym() {
        OtherGymFixture fixture = createOtherGymDraft("Full");

        assertNotFound(() -> routineService.getFull(GYM_A_ID, fixture.routine().id()));
    }

    @Test
    void updateDoesNotModifyRoutineFromAnotherGym() {
        OtherGymFixture fixture = createOtherGymDraft("Update");
        ExerciseResponse ownExercise = createExercise(GYM_A_ID, "Routine Update Own Exercise " + uniqueSuffix());

        assertNotFound(() -> routineService.update(
                GYM_A_ID,
                fixture.routine().id(),
                updateRequest("Changed " + uniqueSuffix(), ownExercise.id())));
    }

    @Test
    void duplicateDoesNotCopyRoutineFromAnotherGym() {
        OtherGymFixture fixture = createOtherGymDraft("Duplicate");
        StudentResponse ownStudent = createStudent(GYM_A_ID, "Own", "RoutineDuplicate", "a-routine-duplicate-" + uniqueSuffix());

        assertNotFound(() -> routineService.duplicate(
                GYM_A_ID,
                USER_A_ID,
                fixture.routine().id(),
                new DuplicateRoutineRequest(ownStudent.id(), "Copy " + uniqueSuffix(), LocalDate.now(), RoutineStatus.DRAFT)));
    }

    @Test
    void finishDoesNotModifyRoutineFromAnotherGym() {
        OtherGymFixture fixture = createOtherGymDraft("Finish");

        assertNotFound(() -> routineService.finishRoutine(
                GYM_A_ID,
                USER_A_ID,
                fixture.routine().id(),
                new FinishRoutineRequest("Cross-tenant attempt")));
    }

    @Test
    void archiveDoesNotModifyRoutineFromAnotherGym() {
        OtherGymFixture fixture = createOtherGymDraft("Archive");

        // Tenant scoping in getFull must return NotFound before the DRAFT state guard runs.
        assertNotFound(() -> routineService.archiveRoutine(GYM_A_ID, USER_A_ID, fixture.routine().id()));
    }

    @Test
    void activateDoesNotModifyRoutineFromAnotherGym() {
        OtherGymFixture fixture = createOtherGymDraft("Activate");

        // Tenant scoping in getFull must return NotFound before the DRAFT state guard runs.
        assertNotFound(() -> routineService.activateRoutine(GYM_A_ID, USER_A_ID, fixture.routine().id()));
    }

    @Test
    void deleteDoesNotRemoveRoutineFromAnotherGym() {
        OtherGymFixture fixture = createOtherGymDraft("Delete");

        assertNotFound(() -> routineService.delete(GYM_A_ID, fixture.routine().id()));
    }

    @Test
    void finishAndCreateNextDoesNotUseRoutineFromAnotherGym() {
        OtherGymFixture fixture = createOtherGymDraft("FinishNext");

        assertNotFound(() -> routineLifecycleService.finishAndCreateNext(
                GYM_A_ID,
                USER_A_ID,
                new FinishAndCreateNextRequest(
                        fixture.routine().id(),
                        null,
                        "Next " + uniqueSuffix(),
                        LocalDate.now().plusWeeks(4),
                        "DRAFT",
                        true,
                        false,
                        null)));
    }

    @Test
    void createNextDoesNotUseRoutineFromAnotherGym() {
        OtherGymFixture fixture = createOtherGymDraft("CreateNext");

        assertNotFound(() -> routineLifecycleService.createNextFromExisting(
                GYM_A_ID,
                USER_A_ID,
                fixture.routine().id(),
                new CreateNextRoutineRequest(
                        "Next " + uniqueSuffix(),
                        LocalDate.now().plusWeeks(4),
                        "DRAFT",
                        true,
                        false,
                        null)));
    }

    @Test
    void getActiveDoesNotExposeRoutineForStudentFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Routine Active");
        User otherUser = createUser(otherGym, "routine-active-other-" + uniqueSuffix() + "@gymplanner.local");
        StudentResponse otherStudent = createStudent(
                otherGym.getId(), "Other", "RoutineActive", "b-routine-active-" + uniqueSuffix());
        ExerciseResponse otherExercise = createExercise(
                otherGym.getId(), "Routine Active Other Exercise " + uniqueSuffix());
        createActiveRoutine(otherGym.getId(), otherUser.getId(), otherStudent.id(), otherExercise.id(), "Other Active " + uniqueSuffix());

        assertNotFound(() -> routineService.getActive(GYM_A_ID, otherStudent.id()));
    }

    @Test
    void listForStudentDoesNotIncludeRoutineFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Routine Student List");
        User otherUser = createUser(otherGym, "routine-student-list-other-" + uniqueSuffix() + "@gymplanner.local");
        StudentResponse ownStudent = createStudent(
                GYM_A_ID, "Own", "RoutineStudentList", "a-routine-student-list-" + uniqueSuffix());
        StudentResponse otherStudent = createStudent(
                otherGym.getId(), "Other", "RoutineStudentList", "b-routine-student-list-" + uniqueSuffix());
        RoutineResponse ownRoutine = createDraftRoutine(
                GYM_A_ID, USER_A_ID, ownStudent.id(), "Own Student Routine " + uniqueSuffix());
        RoutineResponse otherRoutine = createDraftRoutine(
                otherGym.getId(), otherUser.getId(), otherStudent.id(), "Other Student Routine " + uniqueSuffix());

        var result = routineService.listForStudent(
                GYM_A_ID, ownStudent.id(), "DRAFT", PageRequest.of(0, 20));

        assertThat(result.content())
                .extracting(RoutineSummaryResponse::id)
                .contains(ownRoutine.id())
                .doesNotContain(otherRoutine.id());
    }

    @Test
    void listForStudentRejectsStudentFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Routine Foreign Student List");
        User otherUser = createUser(otherGym, "routine-foreign-student-list-" + uniqueSuffix() + "@gymplanner.local");
        StudentResponse otherStudent = createStudent(
                otherGym.getId(), "Other", "RoutineForeignList", "b-routine-foreign-list-" + uniqueSuffix());
        createDraftRoutine(otherGym.getId(), otherUser.getId(), otherStudent.id(), "Other Foreign List " + uniqueSuffix());

        assertNotFound(() -> routineService.listForStudent(
                GYM_A_ID, otherStudent.id(), "DRAFT", PageRequest.of(0, 20)));
    }

    @Test
    void globalListDoesNotIncludeRoutineFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Routine Global List");
        User otherUser = createUser(otherGym, "routine-global-list-other-" + uniqueSuffix() + "@gymplanner.local");
        String suffix = uniqueSuffix();
        StudentResponse ownStudent = createStudent(
                GYM_A_ID, "Own", "RoutineGlobalList", "a-routine-global-list-" + suffix);
        StudentResponse otherStudent = createStudent(
                otherGym.getId(), "Other", "RoutineGlobalList", "b-routine-global-list-" + suffix);
        RoutineResponse ownRoutine = createDraftRoutine(
                GYM_A_ID, USER_A_ID, ownStudent.id(), "Cross Routine Global Own " + suffix);
        RoutineResponse otherRoutine = createDraftRoutine(
                otherGym.getId(), otherUser.getId(), otherStudent.id(), "Cross Routine Global Other " + suffix);

        var result = routineService.list(
                GYM_A_ID,
                "DRAFT",
                "Cross Routine Global",
                null,
                null,
                null,
                null,
                PageRequest.of(0, 20));

        assertThat(result.content())
                .extracting(RoutineSummaryResponse::id)
                .contains(ownRoutine.id())
                .doesNotContain(otherRoutine.id());
    }

    @Test
    void createFromScratchRejectsStudentFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Routine Foreign Student Create");
        StudentResponse otherStudent = createStudent(
                otherGym.getId(), "Other", "RoutineCreate", "b-routine-create-" + uniqueSuffix());

        assertNotFound(() -> routineService.createFromScratch(
                GYM_A_ID,
                USER_A_ID,
                draftRequest(otherStudent.id(), "Routine Foreign Student " + uniqueSuffix())));
    }

    @Test
    void createFromScratchRejectsExerciseFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Routine Foreign Exercise Create");
        StudentResponse ownStudent = createStudent(
                GYM_A_ID, "Own", "RoutineExerciseCreate", "a-routine-exercise-create-" + uniqueSuffix());
        ExerciseResponse otherExercise = createExercise(
                otherGym.getId(), "Routine Foreign Exercise " + uniqueSuffix());

        assertNotFound(() -> routineService.createFromScratch(
                GYM_A_ID,
                USER_A_ID,
                activeRequest(ownStudent.id(), "Routine With Foreign Exercise " + uniqueSuffix(), otherExercise.id())));
    }

    @Test
    void createFromTemplateRejectsTemplateFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Routine Foreign Template");
        User otherUser = createUser(otherGym, "routine-template-other-" + uniqueSuffix() + "@gymplanner.local");
        StudentResponse ownStudent = createStudent(
                GYM_A_ID, "Own", "RoutineTemplate", "a-routine-template-" + uniqueSuffix());
        ExerciseResponse otherExercise = createExercise(
                otherGym.getId(), "Routine Template Other Exercise " + uniqueSuffix());
        TemplateResponse otherTemplate = createTemplate(
                otherGym.getId(), otherUser.getId(), "Routine Other Template " + uniqueSuffix(), otherExercise.id());

        assertNotFound(() -> routineFromTemplateService.createFromTemplate(
                GYM_A_ID,
                USER_A_ID,
                fromTemplateRequest(ownStudent.id(), otherTemplate.id(), "Routine Foreign Template " + uniqueSuffix())));
    }

    @Test
    void createFromTemplateRejectsStudentFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Routine Template Foreign Student");
        StudentResponse otherStudent = createStudent(
                otherGym.getId(), "Other", "RoutineTemplateStudent", "b-routine-template-student-" + uniqueSuffix());
        ExerciseResponse ownExercise = createExercise(
                GYM_A_ID, "Routine Template Own Exercise " + uniqueSuffix());
        TemplateResponse ownTemplate = createTemplate(
                GYM_A_ID, USER_A_ID, "Routine Own Template " + uniqueSuffix(), ownExercise.id());

        assertNotFound(() -> routineFromTemplateService.createFromTemplate(
                GYM_A_ID,
                USER_A_ID,
                fromTemplateRequest(otherStudent.id(), ownTemplate.id(), "Routine Foreign Student " + uniqueSuffix())));
    }

    private OtherGymFixture createOtherGymDraft(String label) {
        String suffix = uniqueSuffix();
        Gym gym = createOtherGym("Other Gym Routine " + label + " " + suffix);
        User user = createUser(gym, "routine-" + label.toLowerCase() + "-" + suffix + "@gymplanner.local");
        StudentResponse student = createStudent(
                gym.getId(), "Other", "Routine" + label, "b-routine-" + label.toLowerCase() + "-" + suffix);
        RoutineResponse routine = createDraftRoutine(
                gym.getId(), user.getId(), student.id(), "Other Routine " + label + " " + suffix);
        return new OtherGymFixture(gym, user, student, routine);
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

    private RoutineResponse createDraftRoutine(Long gymId, Long userId, Long studentId, String name) {
        return routineService.createFromScratch(gymId, userId, draftRequest(studentId, name));
    }

    private RoutineResponse createActiveRoutine(
            Long gymId, Long userId, Long studentId, Long exerciseId, String name) {
        return routineService.createFromScratch(gymId, userId, activeRequest(studentId, name, exerciseId));
    }

    private CreateRoutineFromScratchRequest draftRequest(Long studentId, String name) {
        return new CreateRoutineFromScratchRequest(
                studentId,
                name,
                null,
                RoutineStatus.DRAFT,
                LocalDate.now(),
                null,
                null,
                List.of());
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
                routineDays(exerciseId));
    }

    private UpdateRoutineRequest updateRequest(String name, Long exerciseId) {
        return new UpdateRoutineRequest(
                name,
                "Fuerza",
                RoutineStatus.DRAFT,
                LocalDate.now(),
                null,
                null,
                routineDays(exerciseId));
    }

    private List<RoutineDayInput> routineDays(Long exerciseId) {
        return List.of(new RoutineDayInput(
                null,
                null,
                "Dia 1",
                null,
                List.of(
                        routineBlock("Warmup", BlockPurpose.WARMUP, exerciseId),
                        routineBlock("Main", BlockPurpose.MAIN_LIFT, exerciseId),
                        routineBlock("Cooldown", BlockPurpose.COOLDOWN, exerciseId))));
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

    private TemplateResponse createTemplate(Long gymId, Long userId, String name, Long exerciseId) {
        return templateService.create(
                gymId,
                userId,
                new CreateTemplateRequest(
                        name,
                        null,
                        "Futbol",
                        "Fuerza",
                        "Intermedio",
                        45,
                        null,
                        templateDays(exerciseId)));
    }

    private List<TemplateDayInput> templateDays(Long exerciseId) {
        return List.of(new TemplateDayInput(
                null,
                null,
                "Dia 1",
                null,
                List.of(
                        templateBlock("Warmup", BlockPurpose.WARMUP, exerciseId),
                        templateBlock("Main", BlockPurpose.MAIN_LIFT, exerciseId),
                        templateBlock("Cooldown", BlockPurpose.COOLDOWN, exerciseId))));
    }

    private TemplateBlockInput templateBlock(String title, BlockPurpose purpose, Long exerciseId) {
        return new TemplateBlockInput(
                null,
                title,
                BlockStructuralType.STANDARD,
                purpose,
                null,
                null,
                null,
                null,
                List.of(new TemplateExerciseInput(
                        exerciseId,
                        null,
                        null,
                        List.of(new TemplateExerciseSetInput(
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

    private CreateRoutineFromTemplateRequest fromTemplateRequest(Long studentId, Long templateId, String name) {
        return new CreateRoutineFromTemplateRequest(
                studentId,
                templateId,
                name,
                LocalDate.now(),
                null,
                null,
                RoutineStatus.DRAFT);
    }

    private void assertNotFound(ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isExactlyInstanceOf(NotFoundException.class);
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
}

    private record OtherGymFixture(Gym gym, User user, StudentResponse student, RoutineResponse routine) {}
}
