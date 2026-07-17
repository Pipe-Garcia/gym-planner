package com.gymplanner.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymplanner.exercise.ExerciseService;
import com.gymplanner.exercise.MeasurementType;
import com.gymplanner.exercise.dto.CreateExerciseRequest;
import com.gymplanner.exercise.dto.ExerciseResponse;
import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymRepository;
import com.gymplanner.shared.blocks.BlockPurpose;
import com.gymplanner.shared.blocks.BlockStructuralType;
import com.gymplanner.shared.blocks.SetKind;
import com.gymplanner.shared.exception.NotFoundException;
import com.gymplanner.support.PostgresIntegrationTest;
import com.gymplanner.template.dto.CreateTemplateRequest;
import com.gymplanner.template.dto.TemplateBlockInput;
import com.gymplanner.template.dto.TemplateDayInput;
import com.gymplanner.template.dto.TemplateExerciseInput;
import com.gymplanner.template.dto.TemplateExerciseSetInput;
import com.gymplanner.template.dto.TemplateResponse;
import com.gymplanner.template.dto.TemplateSummaryResponse;
import com.gymplanner.template.dto.UpdateTemplateRequest;
import com.gymplanner.user.User;
import com.gymplanner.user.UserRepository;
import com.gymplanner.user.UserRole;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class TemplateCrossTenantIntegrationTest extends PostgresIntegrationTest {

    private static final Long GYM_A_ID = 1L;
    private static final Long USER_A_ID = 1L;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private GymRepository gymRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void getDoesNotExposeTemplateFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Template Get");
        User otherUser = createUser(otherGym, "template-get-other@gymplanner.local");
        ExerciseResponse otherExercise = createExercise(otherGym.getId(), "Template Get Exercise " + uniqueSuffix());
        TemplateResponse otherTemplate = createTemplate(otherGym.getId(), otherUser.getId(), "Cross Get " + uniqueSuffix(), otherExercise.id());

        assertNotFound(() -> templateService.get(GYM_A_ID, otherTemplate.id()));
    }

    @Test
    void getFullDoesNotExposeTemplateFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Template Full");
        User otherUser = createUser(otherGym, "template-full-other@gymplanner.local");
        ExerciseResponse otherExercise = createExercise(otherGym.getId(), "Template Full Exercise " + uniqueSuffix());
        TemplateResponse otherTemplate = createTemplate(otherGym.getId(), otherUser.getId(), "Cross Full " + uniqueSuffix(), otherExercise.id());

        assertNotFound(() -> templateService.getFull(GYM_A_ID, otherTemplate.id()));
    }

    @Test
    void getEntityDoesNotExposeTemplateFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Template Entity");
        User otherUser = createUser(otherGym, "template-entity-other@gymplanner.local");
        ExerciseResponse otherExercise = createExercise(otherGym.getId(), "Template Entity Exercise " + uniqueSuffix());
        TemplateResponse otherTemplate = createTemplate(otherGym.getId(), otherUser.getId(), "Cross Entity " + uniqueSuffix(), otherExercise.id());

        assertNotFound(() -> templateService.getEntity(GYM_A_ID, otherTemplate.id()));
    }

    @Test
    void updateDoesNotModifyTemplateFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Template Update");
        User otherUser = createUser(otherGym, "template-update-other@gymplanner.local");
        ExerciseResponse otherExercise = createExercise(otherGym.getId(), "Template Update Exercise " + uniqueSuffix());
        TemplateResponse otherTemplate = createTemplate(otherGym.getId(), otherUser.getId(), "Cross Update " + uniqueSuffix(), otherExercise.id());

        assertNotFound(() -> templateService.update(GYM_A_ID, otherTemplate.id(), updateRequest("Changed " + uniqueSuffix(), otherExercise.id())));
    }

    @Test
    void deactivateDoesNotAffectTemplateFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Template Deactivate");
        User otherUser = createUser(otherGym, "template-deactivate-other@gymplanner.local");
        ExerciseResponse otherExercise = createExercise(otherGym.getId(), "Template Deactivate Exercise " + uniqueSuffix());
        TemplateResponse otherTemplate = createTemplate(otherGym.getId(), otherUser.getId(), "Cross Deactivate " + uniqueSuffix(), otherExercise.id());

        assertNotFound(() -> templateService.deactivate(GYM_A_ID, otherTemplate.id()));
    }

    @Test
    void reactivateDoesNotAffectTemplateFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Template Reactivate");
        User otherUser = createUser(otherGym, "template-reactivate-other@gymplanner.local");
        ExerciseResponse otherExercise = createExercise(otherGym.getId(), "Template Reactivate Exercise " + uniqueSuffix());
        TemplateResponse otherTemplate = createTemplate(otherGym.getId(), otherUser.getId(), "Cross Reactivate " + uniqueSuffix(), otherExercise.id());
        templateService.deactivate(otherGym.getId(), otherTemplate.id());

        assertNotFound(() -> templateService.reactivate(GYM_A_ID, otherTemplate.id()));
    }

    @Test
    void duplicateDoesNotCopyTemplateFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Template Duplicate");
        User otherUser = createUser(otherGym, "template-duplicate-other@gymplanner.local");
        ExerciseResponse otherExercise = createExercise(otherGym.getId(), "Template Duplicate Exercise " + uniqueSuffix());
        TemplateResponse otherTemplate = createTemplate(otherGym.getId(), otherUser.getId(), "Cross Duplicate " + uniqueSuffix(), otherExercise.id());

        assertNotFound(() -> templateService.duplicate(GYM_A_ID, USER_A_ID, otherTemplate.id()));
    }

    @Test
    void listDoesNotIncludeTemplatesFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Template List");
        User otherUser = createUser(otherGym, "template-list-other@gymplanner.local");
        String suffix = uniqueSuffix();
        ExerciseResponse ownExercise = createExercise(GYM_A_ID, "Template List Own Exercise " + suffix);
        ExerciseResponse otherExercise = createExercise(otherGym.getId(), "Template List Other Exercise " + suffix);
        TemplateResponse ownTemplate = createTemplate(GYM_A_ID, USER_A_ID, "Cross Template List Own " + suffix, ownExercise.id());
        TemplateResponse otherTemplate = createTemplate(otherGym.getId(), otherUser.getId(), "Cross Template List Other " + suffix, otherExercise.id());

        var result = templateService.list(GYM_A_ID, "Cross Template List", null, null, null, true, PageRequest.of(0, 20));

        assertThat(result.content())
                .extracting(TemplateSummaryResponse::id)
                .contains(ownTemplate.id())
                .doesNotContain(otherTemplate.id());
    }

    @Test
    void createRejectsExerciseFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Template Foreign Exercise Create");
        ExerciseResponse otherExercise = createExercise(otherGym.getId(), "Template Foreign Create Exercise " + uniqueSuffix());

        assertNotFound(() -> templateService.create(
                GYM_A_ID,
                USER_A_ID,
                createRequest("Template With Foreign Exercise " + uniqueSuffix(), otherExercise.id())));
    }

    @Test
    void updateRejectsExerciseFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Template Foreign Exercise Update");
        ExerciseResponse ownExercise = createExercise(GYM_A_ID, "Template Own Update Exercise " + uniqueSuffix());
        ExerciseResponse otherExercise = createExercise(otherGym.getId(), "Template Foreign Update Exercise " + uniqueSuffix());
        TemplateResponse ownTemplate = createTemplate(GYM_A_ID, USER_A_ID, "Own Template Foreign Update " + uniqueSuffix(), ownExercise.id());

        assertNotFound(() -> templateService.update(
                GYM_A_ID,
                ownTemplate.id(),
                updateRequest("Own Template Foreign Update Changed " + uniqueSuffix(), otherExercise.id())));
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

    private TemplateResponse createTemplate(Long gymId, Long userId, String name, Long exerciseId) {
        return templateService.create(gymId, userId, createRequest(name, exerciseId));
    }

    private CreateTemplateRequest createRequest(String name, Long exerciseId) {
        return new CreateTemplateRequest(
                name,
                null,
                "Futbol",
                "Fuerza",
                "Intermedio",
                45,
                null,
                templateDays(exerciseId));
    }

    private UpdateTemplateRequest updateRequest(String name, Long exerciseId) {
        return new UpdateTemplateRequest(
                name,
                null,
                "Futbol",
                "Fuerza",
                "Intermedio",
                45,
                null,
                true,
                templateDays(exerciseId));
    }

    private List<TemplateDayInput> templateDays(Long exerciseId) {
        return List.of(new TemplateDayInput(
                null,
                null,
                "Dia 1",
                null,
                List.of(
                        block("Warmup", BlockPurpose.WARMUP, exerciseId),
                        block("Main", BlockPurpose.MAIN_LIFT, exerciseId),
                        block("Cooldown", BlockPurpose.COOLDOWN, exerciseId))));
    }

    private TemplateBlockInput block(String title, BlockPurpose purpose, Long exerciseId) {
        return new TemplateBlockInput(
                null,
                title,
                BlockStructuralType.STANDARD,
                purpose,
                null,
                null,
                null,
                null,
                List.of(templateExercise(exerciseId)));
    }

    private TemplateExerciseInput templateExercise(Long exerciseId) {
        return new TemplateExerciseInput(
                exerciseId,
                null,
                null,
                List.of(templateSet()));
    }

    private TemplateExerciseSetInput templateSet() {
        return new TemplateExerciseSetInput(
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
                false);
    }

    private void assertNotFound(ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(NotFoundException.class);
    }

    private String uniqueSuffix() {
        return Long.toString(System.nanoTime());
    }
}
