package com.gymplanner.exercise.tag;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymplanner.exercise.tag.dto.CreateTagRequest;
import com.gymplanner.exercise.tag.dto.TagResponse;
import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymRepository;
import com.gymplanner.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ExerciseTagCrossTenantIntegrationTest extends PostgresIntegrationTest {

    private static final Long GYM_A_ID = 1L;

    @Autowired
    private ExerciseTagService tagService;

    @Autowired
    private GymRepository gymRepository;

    @Test
    void listDoesNotIncludeTagsFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Tag List");
        TagResponse otherTag = createTag(otherGym.getId(), TagType.OBJECTIVE, "Other Listed Tag " + uniqueSuffix());

        var result = tagService.list(GYM_A_ID, null);

        assertThat(result)
                .extracting(TagResponse::name)
                .contains("Fuerza")
                .doesNotContain(otherTag.name());
    }

    @Test
    void sameTypeAndNameInAnotherGymDoesNotBlockCreate() {
        Gym otherGym = createOtherGym("Other Gym Tag Slug Isolation");
        String name = "Shared Tag " + uniqueSuffix();
        TagResponse otherTag = createTag(otherGym.getId(), TagType.LEVEL, name);

        TagResponse ownTag = createTag(GYM_A_ID, TagType.LEVEL, name);

        assertThat(ownTag.name()).isEqualTo(name);
        assertThat(ownTag.type()).isEqualTo(otherTag.type());
        assertThat(ownTag.id()).isNotEqualTo(otherTag.id());
    }

    private Gym createOtherGym(String name) {
        Gym gym = new Gym();
        gym.setName(name);
        return gymRepository.save(gym);
    }

    private TagResponse createTag(Long gymId, TagType type, String name) {
        return tagService.create(gymId, new CreateTagRequest(name, type));
    }

    private String uniqueSuffix() {
        return Long.toString(System.nanoTime());
    }
}
