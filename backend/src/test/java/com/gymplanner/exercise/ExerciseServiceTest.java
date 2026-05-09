package com.gymplanner.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymplanner.exercise.dto.CreateExerciseRequest;
import com.gymplanner.exercise.tag.ExerciseTag;
import com.gymplanner.exercise.tag.ExerciseTagRepository;
import com.gymplanner.exercise.tag.TagType;
import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymRepository;
import com.gymplanner.shared.exception.BusinessRuleException;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExerciseServiceTest {

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private ExerciseTagRepository tagRepository;

    @Autowired
    private GymRepository gymRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createExerciseWithValidTagsAssociatesThem() {
        List<Long> tagIds = tagRepository.findByGymIdOrderByTypeAscNameAsc(1L).stream()
                .limit(2)
                .map(ExerciseTag::getId)
                .toList();

        var response = exerciseService.create(1L, request("Sentadilla", tagIds));

        assertThat(response.tags()).hasSize(2);
        assertThat(exerciseRepository.findById(response.id()).orElseThrow().getTags()).hasSize(2);
    }

    @Test
    void createExerciseWithTagFromAnotherGymThrowsValidationException() {
        Gym otherGym = createOtherGym(99L, "Other Gym");
        ExerciseTag tag = new ExerciseTag();
        tag.setGym(otherGym);
        tag.setType(TagType.LEVEL);
        tag.setName("Especial");
        tag.setSlug("especial");
        tag = tagRepository.save(tag);

        Long tagId = tag.getId();
        assertThatThrownBy(() -> exerciseService.create(1L, request("Sentadilla", List.of(tagId))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void filterByMultipleTagsUsesAndLogic() {
        List<ExerciseTag> tags = tagRepository.findByGymIdOrderByTypeAscNameAsc(1L);
        Long firstTag = tags.get(0).getId();
        Long secondTag = tags.get(1).getId();
        exerciseService.create(1L, request("Con ambos", List.of(firstTag, secondTag)));
        exerciseService.create(1L, request("Con uno", List.of(firstTag)));

        var response = exerciseService.list(1L, null, List.of(firstTag, secondTag), true, PageRequest.of(0, 10));

        assertThat(response.content()).extracting("name").containsExactly("Con ambos");
    }

    @Test
    void softDeleteMarksInactive() {
        var response = exerciseService.create(1L, request("Sentadilla", List.of()));

        exerciseService.deactivate(1L, response.id());

        assertThat(exerciseRepository.findById(response.id()).orElseThrow().isActive()).isFalse();
    }

    private CreateExerciseRequest request(String name, List<Long> tagIds) {
        return new CreateExerciseRequest(name, "Descripcion", "Notas", MeasurementType.REPS_WEIGHT, null, null, tagIds);
    }

    private Gym createOtherGym(Long id, String name) {
        entityManager.createNativeQuery("INSERT INTO gyms (id, name) VALUES (?, ?)")
                .setParameter(1, id)
                .setParameter(2, name)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        return gymRepository.findById(id).orElseThrow();
    }
}
