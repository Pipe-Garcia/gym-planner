package com.gymplanner.routine;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymplanner.auth.CustomUserDetailsService.GymPrincipal;
import com.gymplanner.exercise.ExerciseService;
import com.gymplanner.exercise.MeasurementType;
import com.gymplanner.exercise.dto.CreateExerciseRequest;
import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymRepository;
import com.gymplanner.routine.dto.CreateRoutineFromScratchRequest;
import com.gymplanner.routine.dto.RoutineBlockInput;
import com.gymplanner.routine.dto.RoutineDayInput;
import com.gymplanner.routine.dto.RoutineExerciseInput;
import com.gymplanner.routine.dto.RoutineExerciseSetInput;
import com.gymplanner.shared.blocks.BlockPurpose;
import com.gymplanner.shared.blocks.BlockStructuralType;
import com.gymplanner.shared.blocks.SetKind;
import com.gymplanner.student.StudentService;
import com.gymplanner.student.dto.CreateStudentRequest;
import com.gymplanner.user.UserRepository;
import com.gymplanner.user.UserRole;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RoutineControllerCrossTenantTest {

    private static final Long GYM_A_ID = 1L;
    private static final Long USER_A_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

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

    @Autowired
    private EntityManager entityManager;

    @Test
    void getRoutineFromAnotherGymReturnsNotFound() throws Exception {
        Long otherRoutineId = createOtherGymRoutine();

        mockMvc.perform(get("/api/routines/{id}", otherRoutineId).with(user(principal())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRoutinePdfFromAnotherGymReturnsNotFound() throws Exception {
        Long otherRoutineId = createOtherGymRoutine();

        mockMvc.perform(get("/api/routines/{id}/pdf", otherRoutineId).with(user(principal())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRoutineTextFromAnotherGymReturnsNotFound() throws Exception {
        Long otherRoutineId = createOtherGymRoutine();

        mockMvc.perform(get("/api/routines/{id}/text", otherRoutineId).with(user(principal())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRoutineFromPrincipalGymReturnsOk() throws Exception {
        Long ownRoutineId = createRoutineFixture(GYM_A_ID, USER_A_ID);

        mockMvc.perform(get("/api/routines/{id}", ownRoutineId).with(user(principal())))
                .andExpect(status().isOk());
    }

    private Long createOtherGymRoutine() {
        Gym otherGym = createOtherGym();
        Long otherUserId = createUser(otherGym.getId());
        return createRoutineFixture(otherGym.getId(), otherUserId);
    }

    private Long createRoutineFixture(Long gymId, Long userId) {
        String suffix = uniqueSuffix();
        Long studentId = studentService.create(gymId, new CreateStudentRequest(
                "Student",
                "HTTP " + suffix,
                null,
                null,
                null,
                LocalDate.of(2000, 1, 1),
                "Voley",
                "Saltabilidad",
                "Intermedio",
                null,
                LocalDate.now())).id();
        Long exerciseId = exerciseService.create(gymId, new CreateExerciseRequest(
                "HTTP Exercise " + suffix,
                null,
                null,
                MeasurementType.REPS_WEIGHT,
                null,
                null,
                List.of())).id();
        return routineService.createFromScratch(gymId, userId, new CreateRoutineFromScratchRequest(
                studentId,
                "HTTP Routine " + suffix,
                "Saltabilidad",
                RoutineStatus.ACTIVE,
                LocalDate.now(),
                "General notes",
                "Private notes",
                List.of(day(exerciseId)))).id();
    }

    private RoutineDayInput day(Long exerciseId) {
        return new RoutineDayInput(null, null, "Dia 1", null, List.of(
                block("Warmup", BlockPurpose.WARMUP, exerciseId),
                block("Main", BlockPurpose.MAIN_LIFT, exerciseId),
                block("Cooldown", BlockPurpose.COOLDOWN, exerciseId)));
    }

    private RoutineBlockInput block(String title, BlockPurpose purpose, Long exerciseId) {
        return new RoutineBlockInput(null, title, BlockStructuralType.STANDARD, purpose, null, null, null, List.of(
                new RoutineExerciseInput(exerciseId, null, null, List.of(
                        new RoutineExerciseSetInput(
                                null,
                                SetKind.NORMAL,
                                10,
                                null,
                                null,
                                new BigDecimal("20"),
                                null,
                                null,
                                60,
                                null,
                                null,
                                null,
                                false)))));
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

    private Long createUser(Long gymId) {
        Long id = ((Number) entityManager.createNativeQuery("SELECT COALESCE(MAX(id), 0) + 1 FROM users")
                .getSingleResult()).longValue();
        entityManager.createNativeQuery("""
                        INSERT INTO users (id, gym_id, email, password_hash, full_name, role, active, created_at, updated_at)
                        VALUES (:id, :gymId, :email, 'hash', 'Other Owner', 'OWNER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """)
                .setParameter("id", id)
                .setParameter("gymId", gymId)
                .setParameter("email", "u" + uniqueSuffix() + "@test.local")
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        return id;
    }

    private String uniqueSuffix() {
        return Long.toString(System.nanoTime());
    }

    private GymPrincipal principal() {
        return new GymPrincipal(1L, "admin@gymplanner.local", "password", "Owner Demo", UserRole.OWNER, 1L, true);
    }
}
