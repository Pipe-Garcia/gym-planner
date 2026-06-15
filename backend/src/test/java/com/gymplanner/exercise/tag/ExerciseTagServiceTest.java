package com.gymplanner.exercise.tag;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymplanner.exercise.ExerciseService;
import com.gymplanner.exercise.MeasurementType;
import com.gymplanner.exercise.dto.CreateExerciseRequest;
import com.gymplanner.exercise.tag.dto.TagUsageResponse;
import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExerciseTagServiceTest {

    @Autowired
    private ExerciseTagService tagService;

    @Autowired
    private ExerciseTagRepository tagRepository;

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private GymRepository gymRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void listUsageReturnsCountsZeroUsagesAndIsolatesGyms() {
        Gym currentGym = gymRepository.findById(1L).orElseThrow();
        Gym otherGym = createOtherGym();

        ExerciseTag usedTag = createTag(currentGym, TagType.OBJECTIVE, "Uso actual");
        ExerciseTag unusedTag = createTag(currentGym, TagType.OBJECTIVE, "Sin uso");
        ExerciseTag otherTypeTag = createTag(currentGym, TagType.LEVEL, "Nivel actual");
        ExerciseTag otherGymTag = createTag(otherGym, TagType.OBJECTIVE, "Uso externo");

        createExercise(1L, "Ejercicio activo", List.of(usedTag.getId()));
        Long inactiveExerciseId = createExercise(1L, "Ejercicio inactivo", List.of(usedTag.getId()));
        exerciseService.deactivate(1L, inactiveExerciseId);

        Long otherExerciseId = createExercise(otherGym.getId(), "Ejercicio externo", List.of(otherGymTag.getId()));
        entityManager.createNativeQuery("""
                        INSERT INTO exercise_tag_assignments (exercise_id, tag_id)
                        VALUES (:exerciseId, :tagId)
                        """)
                .setParameter("exerciseId", otherExerciseId)
                .setParameter("tagId", usedTag.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        List<TagUsageResponse> result = tagService.listUsage(1L, null);

        assertThat(result).filteredOn(tag -> tag.id().equals(usedTag.getId()))
                .singleElement()
                .extracting(TagUsageResponse::usageCount)
                .isEqualTo(2L);
        assertThat(result).filteredOn(tag -> tag.id().equals(unusedTag.getId()))
                .singleElement()
                .extracting(TagUsageResponse::usageCount)
                .isEqualTo(0L);
        assertThat(result).extracting(TagUsageResponse::id)
                .contains(otherTypeTag.getId())
                .doesNotContain(otherGymTag.getId());
    }

    @Test
    void listUsageFiltersByType() {
        Gym gym = gymRepository.findById(1L).orElseThrow();
        ExerciseTag objective = createTag(gym, TagType.OBJECTIVE, "Objetivo filtrado");
        ExerciseTag level = createTag(gym, TagType.LEVEL, "Nivel filtrado");

        List<TagUsageResponse> result = tagService.listUsage(1L, TagType.OBJECTIVE);

        assertThat(result).allMatch(tag -> tag.type() == TagType.OBJECTIVE);
        assertThat(result).extracting(TagUsageResponse::id)
                .contains(objective.getId())
                .doesNotContain(level.getId());
    }

    private ExerciseTag createTag(Gym gym, TagType type, String name) {
        String suffix = Long.toString(System.nanoTime());
        ExerciseTag tag = new ExerciseTag();
        tag.setGym(gym);
        tag.setType(type);
        tag.setName(name + " " + suffix);
        tag.setSlug(name.toLowerCase().replace(' ', '-') + "-" + suffix);
        return tagRepository.save(tag);
    }

    private Long createExercise(Long gymId, String name, List<Long> tagIds) {
        return exerciseService.create(gymId, new CreateExerciseRequest(
                name + " " + System.nanoTime(),
                null,
                null,
                MeasurementType.REPS_WEIGHT,
                null,
                null,
                tagIds)).id();
    }

    private Gym createOtherGym() {
        Long id = ((Number) entityManager.createNativeQuery("SELECT COALESCE(MAX(id), 0) + 1 FROM gyms")
                .getSingleResult()).longValue();
        entityManager.createNativeQuery("""
                        INSERT INTO gyms (id, name, created_at, updated_at)
                        VALUES (:id, :name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """)
                .setParameter("id", id)
                .setParameter("name", "Otro gym " + System.nanoTime())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        return gymRepository.findById(id).orElseThrow();
    }
}
