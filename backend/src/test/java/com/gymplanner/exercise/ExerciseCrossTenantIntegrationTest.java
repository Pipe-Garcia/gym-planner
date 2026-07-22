package com.gymplanner.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymplanner.exercise.dto.CreateExerciseRequest;
import com.gymplanner.exercise.dto.ExerciseResponse;
import com.gymplanner.exercise.dto.ExerciseSummaryResponse;
import com.gymplanner.exercise.dto.UpdateExerciseRequest;
import com.gymplanner.exercise.tag.ExerciseTagService;
import com.gymplanner.exercise.tag.TagType;
import com.gymplanner.exercise.tag.dto.CreateTagRequest;
import com.gymplanner.exercise.tag.dto.TagResponse;
import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymRepository;
import com.gymplanner.shared.exception.BusinessRuleException;
import com.gymplanner.shared.exception.ConflictException;
import com.gymplanner.shared.exception.NotFoundException;
import com.gymplanner.support.PostgresIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ExerciseCrossTenantIntegrationTest extends PostgresIntegrationTest {

    private static final Long GYM_A_ID = 1L;
    private static final Long MISSING_TAG_ID = 999999L;
    private static final String TAG_OWNERSHIP_MESSAGE = "All tags must exist and belong to the current gym.";

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private ExerciseTagService tagService;

    @Autowired
    private GymRepository gymRepository;

    @Test
    void getDoesNotExposeExerciseFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Exercise Get");
        ExerciseResponse otherExercise = createExercise(otherGym.getId(), "Cross Get " + uniqueSuffix());

        assertThatThrownBy(() -> exerciseService.get(GYM_A_ID, otherExercise.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateDoesNotModifyExerciseFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Exercise Update");
        ExerciseResponse otherExercise = createExercise(otherGym.getId(), "Cross Update " + uniqueSuffix());

        assertThatThrownBy(() -> exerciseService.update(GYM_A_ID, otherExercise.id(), updateRequest("Changed " + uniqueSuffix(), null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deactivateDoesNotAffectExerciseFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Exercise Deactivate");
        ExerciseResponse otherExercise = createExercise(otherGym.getId(), "Cross Deactivate " + uniqueSuffix());

        assertThatThrownBy(() -> exerciseService.deactivate(GYM_A_ID, otherExercise.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void reactivateDoesNotAffectExerciseFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Exercise Reactivate");
        ExerciseResponse otherExercise = createExercise(otherGym.getId(), "Cross Reactivate " + uniqueSuffix());
        exerciseService.deactivate(otherGym.getId(), otherExercise.id());

        assertThatThrownBy(() -> exerciseService.reactivate(GYM_A_ID, otherExercise.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getEntityDoesNotExposeExerciseFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Exercise Entity");
        ExerciseResponse otherExercise = createExercise(otherGym.getId(), "Cross Entity " + uniqueSuffix());

        assertThatThrownBy(() -> exerciseService.getEntity(GYM_A_ID, otherExercise.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listDoesNotIncludeExercisesFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Exercise List");
        String suffix = uniqueSuffix();
        ExerciseResponse ownExercise = createExercise(GYM_A_ID, "Cross List Own " + suffix);
        ExerciseResponse otherExercise = createExercise(otherGym.getId(), "Cross List Other " + suffix);

        var result = exerciseService.list(GYM_A_ID, "Cross List", null, true, PageRequest.of(0, 20));

        assertThat(result.content())
                .extracting(ExerciseSummaryResponse::id)
                .contains(ownExercise.id())
                .doesNotContain(otherExercise.id());
    }

    @Test
    void createRejectsTagFromAnotherGymLikeMissingTag() {
        Gym otherGym = createOtherGym("Other Gym Exercise Foreign Tag Create");
        TagResponse otherTag = createTag(otherGym.getId(), "Foreign Create Tag " + uniqueSuffix());

        assertTagOwnershipViolation(() -> exerciseService.create(
                GYM_A_ID,
                createRequest("Exercise With Foreign Tag " + uniqueSuffix(), List.of(otherTag.id()))));
        assertTagOwnershipViolation(() -> exerciseService.create(
                GYM_A_ID,
                createRequest("Exercise With Missing Tag " + uniqueSuffix(), List.of(MISSING_TAG_ID))));
    }

    @Test
    void updateRejectsTagFromAnotherGymLikeMissingTag() {
        Gym otherGym = createOtherGym("Other Gym Exercise Foreign Tag Update");
        TagResponse otherTag = createTag(otherGym.getId(), "Foreign Update Tag " + uniqueSuffix());
        ExerciseResponse ownExercise = createExercise(GYM_A_ID, "Own Update Foreign Tag " + uniqueSuffix());

        assertTagOwnershipViolation(() -> exerciseService.update(
                GYM_A_ID,
                ownExercise.id(),
                updateRequest("Own Update Foreign Tag Changed " + uniqueSuffix(), List.of(otherTag.id()))));
        assertTagOwnershipViolation(() -> exerciseService.update(
                GYM_A_ID,
                ownExercise.id(),
                updateRequest("Own Update Missing Tag Changed " + uniqueSuffix(), List.of(MISSING_TAG_ID))));
    }

    @Test
    void sameExerciseNameInAnotherGymDoesNotBlockCreate() {
        Gym otherGym = createOtherGym("Other Gym Exercise Slug Isolation");
        String name = "Sentadilla Test " + uniqueSuffix();
        createExercise(otherGym.getId(), name);

        ExerciseResponse ownExercise = createExercise(GYM_A_ID, name);

        assertThat(ownExercise.name()).isEqualTo(name);
    }

    @Test
    void duplicateExerciseNameInSameGymStillConflicts() {
        String name = "Sentadilla Same Gym " + uniqueSuffix();
        createExercise(GYM_A_ID, name);

        assertThatThrownBy(() -> createExercise(GYM_A_ID, name))
                .isInstanceOf(ConflictException.class);
    }

    private Gym createOtherGym(String name) {
        Gym gym = new Gym();
        gym.setName(name);
        return gymRepository.save(gym);
    }

    private ExerciseResponse createExercise(Long gymId, String name) {
        return exerciseService.create(gymId, createRequest(name, null));
    }

    private TagResponse createTag(Long gymId, String name) {
        return tagService.create(gymId, new CreateTagRequest(name, TagType.OBJECTIVE));
    }

    private CreateExerciseRequest createRequest(String name, List<Long> tagIds) {
        return new CreateExerciseRequest(
                name,
                null,
                null,
                MeasurementType.REPS_WEIGHT,
                null,
                null,
                tagIds);
    }

    private UpdateExerciseRequest updateRequest(String name, List<Long> tagIds) {
        return new UpdateExerciseRequest(
                name,
                null,
                null,
                MeasurementType.REPS_WEIGHT,
                null,
                null,
                tagIds);
    }

    private void assertTagOwnershipViolation(ThrowingCallable callable) {
        assertThatThrownBy(callable::call)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(TAG_OWNERSHIP_MESSAGE);
    }

    private String uniqueSuffix() {
        return Long.toString(System.nanoTime());
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call();
    }
}
