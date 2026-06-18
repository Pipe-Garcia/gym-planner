package com.gymplanner.exercise.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymplanner.auth.CustomUserDetailsService.GymPrincipal;
import com.gymplanner.exercise.ExerciseRepository;
import com.gymplanner.exercise.ExerciseService;
import com.gymplanner.exercise.MeasurementType;
import com.gymplanner.exercise.dto.CreateExerciseRequest;
import com.gymplanner.exercise.tag.dto.CreateTagRequest;
import com.gymplanner.exercise.tag.dto.TagResponse;
import com.gymplanner.exercise.tag.dto.UpdateTagRequest;
import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymRepository;
import com.gymplanner.user.UserRole;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExerciseTagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExerciseTagService tagService;

    @Autowired
    private ExerciseTagRepository tagRepository;

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private GymRepository gymRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createTagPersistsGeneratedSlug() throws Exception {
        String suffix = uniqueSuffix();
        String name = "B\u00edceps " + suffix;

        mockMvc.perform(post("/api/exercise-tags")
                        .with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateTagRequest(name, TagType.MUSCLE_GROUP))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.type").value("MUSCLE_GROUP"))
                .andExpect(jsonPath("$.slug").value("biceps-" + suffix));

        assertThat(tagRepository.existsByGymIdAndTypeAndSlug(1L, TagType.MUSCLE_GROUP, "biceps-" + suffix))
                .isTrue();
    }

    @Test
    void createDuplicateSlugInSameGymAndTypeReturnsConflict() throws Exception {
        String suffix = uniqueSuffix();
        tagService.create(1L, new CreateTagRequest("F\u00fatbol " + suffix, TagType.OBJECTIVE));

        mockMvc.perform(post("/api/exercise-tags")
                        .with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateTagRequest("Futbol " + suffix, TagType.OBJECTIVE))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un tag de ese tipo con ese nombre."));
    }

    @Test
    void createSameNameWithDifferentTypeIsAllowed() throws Exception {
        String name = "Compartido " + uniqueSuffix();
        tagService.create(1L, new CreateTagRequest(name, TagType.OBJECTIVE));

        mockMvc.perform(post("/api/exercise-tags")
                        .with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateTagRequest(name, TagType.LEVEL))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("LEVEL"));
    }

    @Test
    void updateTagChangesNameAndSlug() throws Exception {
        TagResponse tag = createTag(1L, TagType.EQUIPMENT, "Equipo inicial");
        String newName = "M\u00e1quina guiada " + uniqueSuffix();

        mockMvc.perform(put("/api/exercise-tags/{id}", tag.id())
                        .with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateTagRequest(newName))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName))
                .andExpect(jsonPath("$.slug").value(newNameSlug(newName)))
                .andExpect(jsonPath("$.type").value("EQUIPMENT"));
    }

    @Test
    void updateTagToDuplicateSlugInSameTypeReturnsConflict() throws Exception {
        String suffix = uniqueSuffix();
        TagResponse target = createTag(1L, TagType.LEVEL, "Nivel origen " + suffix);
        createTag(1L, TagType.LEVEL, "Nivel destino " + suffix);

        mockMvc.perform(put("/api/exercise-tags/{id}", target.id())
                        .with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateTagRequest("Nivel destino " + suffix))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un tag de ese tipo con ese nombre."));
    }

    @Test
    void updateTagKeepingOwnNameDoesNotConflict() throws Exception {
        String name = "Sin cambios " + uniqueSuffix();
        TagResponse tag = createTag(1L, TagType.BODY_AREA, name);

        mockMvc.perform(put("/api/exercise-tags/{id}", tag.id())
                        .with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateTagRequest(name))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tag.id()))
                .andExpect(jsonPath("$.slug").value(tag.slug()));
    }

    @Test
    void deleteUnusedTagReturnsNoContentAndRemovesTag() throws Exception {
        TagResponse tag = createTag(1L, TagType.MOVEMENT_PATTERN, "Sin uso " + uniqueSuffix());

        mockMvc.perform(delete("/api/exercise-tags/{id}", tag.id())
                        .with(user(principal())))
                .andExpect(status().isNoContent());

        assertThat(tagRepository.findByIdAndGymId(tag.id(), 1L)).isEmpty();
    }

    @Test
    void deleteUsedTagRemovesAssignmentsButKeepsExercise() throws Exception {
        TagResponse tag = createTag(1L, TagType.OBJECTIVE, "En uso " + uniqueSuffix());
        Long exerciseId = exerciseService.create(1L, new CreateExerciseRequest(
                "Ejercicio " + uniqueSuffix(),
                null,
                null,
                MeasurementType.REPS_WEIGHT,
                null,
                null,
                List.of(tag.id()))).id();

        mockMvc.perform(delete("/api/exercise-tags/{id}", tag.id())
                        .with(user(principal())))
                .andExpect(status().isNoContent());

        entityManager.clear();
        assertThat(tagRepository.findByIdAndGymId(tag.id(), 1L)).isEmpty();
        assertThat(exerciseRepository.findByIdAndGymId(exerciseId, 1L))
                .isPresent()
                .get()
                .satisfies(exercise -> assertThat(exercise.getTags()).isEmpty());
        assertThat(assignmentCount(exerciseId, tag.id())).isZero();
    }

    @Test
    void updateTagFromAnotherGymReturnsNotFoundAndDoesNotModifyIt() throws Exception {
        Gym otherGym = createOtherGym();
        TagResponse otherTag = createTag(otherGym.getId(), TagType.OBJECTIVE, "Externo");

        mockMvc.perform(put("/api/exercise-tags/{id}", otherTag.id())
                        .with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateTagRequest("Intento de cambio"))))
                .andExpect(status().isNotFound());

        assertThat(tagRepository.findByIdAndGymId(otherTag.id(), otherGym.getId()))
                .get()
                .extracting(ExerciseTag::getName)
                .isEqualTo(otherTag.name());
    }

    @Test
    void deleteTagFromAnotherGymReturnsNotFoundAndKeepsIt() throws Exception {
        Gym otherGym = createOtherGym();
        TagResponse otherTag = createTag(otherGym.getId(), TagType.LEVEL, "Externo");

        mockMvc.perform(delete("/api/exercise-tags/{id}", otherTag.id())
                        .with(user(principal())))
                .andExpect(status().isNotFound());

        assertThat(tagRepository.findByIdAndGymId(otherTag.id(), otherGym.getId())).isPresent();
    }

    @Test
    void createTagWhoseSlugExistsOnlyInAnotherGymIsAllowed() throws Exception {
        Gym otherGym = createOtherGym();
        String name = "Multi tenant " + uniqueSuffix();
        createTag(otherGym.getId(), TagType.OBJECTIVE, name);

        mockMvc.perform(post("/api/exercise-tags")
                        .with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateTagRequest(name, TagType.OBJECTIVE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name));

        assertThat(tagRepository.existsByGymIdAndTypeAndSlug(1L, TagType.OBJECTIVE, newNameSlug(name)))
                .isTrue();
    }

    private TagResponse createTag(Long gymId, TagType type, String prefix) {
        return tagService.create(gymId, new CreateTagRequest(prefix, type));
    }

    private Gym createOtherGym() {
        Long id = ((Number) entityManager.createNativeQuery("SELECT COALESCE(MAX(id), 0) + 1 FROM gyms")
                .getSingleResult()).longValue();
        entityManager.createNativeQuery("""
                        INSERT INTO gyms (id, name, created_at, updated_at)
                        VALUES (:id, :name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """)
                .setParameter("id", id)
                .setParameter("name", "Otro gym " + uniqueSuffix())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        return gymRepository.findById(id).orElseThrow();
    }

    private long assignmentCount(Long exerciseId, Long tagId) {
        return ((Number) entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM exercise_tag_assignments
                        WHERE exercise_id = :exerciseId
                          AND tag_id = :tagId
                        """)
                .setParameter("exerciseId", exerciseId)
                .setParameter("tagId", tagId)
                .getSingleResult()).longValue();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String newNameSlug(String name) {
        return com.gymplanner.shared.util.SlugUtils.toSlug(name);
    }

    private String uniqueSuffix() {
        return Long.toString(System.nanoTime());
    }

    private GymPrincipal principal() {
        return new GymPrincipal(1L, "admin@gymplanner.local", "password", "Owner Demo", UserRole.OWNER, 1L, true);
    }
}
