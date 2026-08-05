package com.gymplanner.student.history;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymplanner.auth.CustomUserDetailsService.GymPrincipal;
import com.gymplanner.exercise.ExerciseService;
import com.gymplanner.exercise.MeasurementType;
import com.gymplanner.exercise.dto.CreateExerciseRequest;
import com.gymplanner.routine.Routine;
import com.gymplanner.routine.RoutineRepository;
import com.gymplanner.routine.RoutineService;
import com.gymplanner.routine.RoutineStatus;
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
class PreviousLoadsControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired StudentService studentService;
    @Autowired ExerciseService exerciseService;
    @Autowired RoutineService routineService;
    @Autowired RoutineRepository routineRepository;
    @Autowired EntityManager entityManager;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/students/{studentId}/exercises/{exerciseId}/previous-loads", 1L, 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returns200WithDataForValidRequest() throws Exception {
        Fixture fixture = fixture();
        Long routineId = createRoutine(fixture, "Historial", LocalDate.of(2026, 6, 1));

        mockMvc.perform(get("/api/students/{studentId}/exercises/{exerciseId}/previous-loads",
                        fixture.studentId(), fixture.exerciseId())
                        .with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exerciseId").value(fixture.exerciseId()))
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.occurrences[0].routineId").value(routineId))
                .andExpect(jsonPath("$.occurrences[0].measurementType").value("REPS_WEIGHT"))
                .andExpect(jsonPath("$.occurrences[0].sets[0].targetReps").value(6));
    }

    @Test
    void returns200WithFoundFalseWhenNoHistory() throws Exception {
        Fixture fixture = fixture();

        mockMvc.perform(get("/api/students/{studentId}/exercises/{exerciseId}/previous-loads",
                        fixture.studentId(), fixture.exerciseId())
                        .with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(false))
                .andExpect(jsonPath("$.occurrences").isEmpty());
    }

    @Test
    void returns404ForStudentInAnotherGym() throws Exception {
        Long otherStudentId = createStudent(otherGymId());
        Long exerciseId = createExercise(1L, MeasurementType.REPS_WEIGHT, "Remo");

        mockMvc.perform(get("/api/students/{studentId}/exercises/{exerciseId}/previous-loads",
                        otherStudentId, exerciseId)
                        .with(user(principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Alumno no encontrado"));
    }

    @Test
    void returns404ForExerciseInAnotherGym() throws Exception {
        Long studentId = createStudent(1L);
        Long otherExerciseId = createExercise(otherGymId(), MeasurementType.REPS_WEIGHT, "Remo externo");

        mockMvc.perform(get("/api/students/{studentId}/exercises/{exerciseId}/previous-loads",
                        studentId, otherExerciseId)
                        .with(user(principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ejercicio no encontrado"));
    }

    @Test
    void appliesLimitQueryParam() throws Exception {
        Fixture fixture = fixture();
        createRoutine(fixture, "Uno", LocalDate.of(2026, 4, 1));
        createRoutine(fixture, "Dos", LocalDate.of(2026, 5, 1));
        createRoutine(fixture, "Tres", LocalDate.of(2026, 6, 1));

        mockMvc.perform(get("/api/students/{studentId}/exercises/{exerciseId}/previous-loads",
                        fixture.studentId(), fixture.exerciseId())
                        .queryParam("limit", "2")
                        .with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occurrences.length()").value(2));
    }

    @Test
    void appliesExcludeRoutineIdParam() throws Exception {
        Fixture fixture = fixture();
        Long older = createRoutine(fixture, "Older", LocalDate.of(2026, 5, 1));
        Long newer = createRoutine(fixture, "Newer", LocalDate.of(2026, 6, 1));

        mockMvc.perform(get("/api/students/{studentId}/exercises/{exerciseId}/previous-loads",
                        fixture.studentId(), fixture.exerciseId())
                        .queryParam("limit", "3")
                        .queryParam("excludeRoutineId", newer.toString())
                        .with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occurrences.length()").value(1))
                .andExpect(jsonPath("$.occurrences[0].routineId").value(older));
    }

    @Test
    void acceptsStructuralTypeQueryParam() throws Exception {
        Fixture fixture = fixture();
        createRoutine(fixture, "Standard", LocalDate.of(2026, 5, 1), BlockStructuralType.STANDARD);
        Long pyramid = createRoutine(fixture, "Pyramid", LocalDate.of(2026, 6, 1), BlockStructuralType.PYRAMID);

        mockMvc.perform(get("/api/students/{studentId}/exercises/{exerciseId}/previous-loads",
                        fixture.studentId(), fixture.exerciseId())
                        .queryParam("limit", "3")
                        .queryParam("structuralType", "PYRAMID")
                        .with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.occurrences.length()").value(1))
                .andExpect(jsonPath("$.occurrences[0].routineId").value(pyramid))
                .andExpect(jsonPath("$.occurrences[0].blockStructuralType").value("PYRAMID"));
    }

    @Test
    void acceptsIncludeFallbackQueryParam() throws Exception {
        Fixture fixture = fixture();
        Long circuit = createRoutine(fixture, "Circuit", LocalDate.of(2026, 6, 1), BlockStructuralType.CIRCUIT);

        mockMvc.perform(get("/api/students/{studentId}/exercises/{exerciseId}/previous-loads",
                        fixture.studentId(), fixture.exerciseId())
                        .queryParam("limit", "1")
                        .queryParam("structuralType", "STANDARD")
                        .queryParam("includeFallback", "true")
                        .with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.matchType").value("DIFFERENT_STRUCTURAL_TYPE"))
                .andExpect(jsonPath("$.requestedStructuralType").value("STANDARD"))
                .andExpect(jsonPath("$.occurrences[0].routineId").value(circuit))
                .andExpect(jsonPath("$.occurrences[0].blockStructuralType").value("CIRCUIT"));
    }

    private Fixture fixture() {
        return new Fixture(
                createStudent(1L),
                createExercise(1L, MeasurementType.REPS_WEIGHT, "Sentadilla"),
                createExercise(1L, MeasurementType.REPS_ONLY, "Movilidad"));
    }

    private Long createStudent(Long gymId) {
        return studentService.create(gymId, new CreateStudentRequest(
                "Julia " + System.nanoTime(),
                "Lopez",
                null,
                "555",
                null,
                null,
                "Voley",
                "Fuerza",
                "Intermedio",
                null,
                LocalDate.now())).id();
    }

    private Long createExercise(Long gymId, MeasurementType measurementType, String name) {
        return exerciseService.create(gymId, new CreateExerciseRequest(
                name + " " + System.nanoTime(),
                "Desc",
                null,
                measurementType,
                null,
                null,
                List.of())).id();
    }

    private Long createRoutine(Fixture fixture, String name, LocalDate finishedDate) {
        return createRoutine(fixture, name, finishedDate, BlockStructuralType.STANDARD);
    }

    private Long createRoutine(Fixture fixture, String name, LocalDate finishedDate, BlockStructuralType structuralType) {
        Long routineId = routineService.createFromScratch(1L, 1L, new CreateRoutineFromScratchRequest(
                fixture.studentId(),
                name,
                "Fuerza",
                RoutineStatus.FINISHED,
                finishedDate.minusWeeks(4),
                null,
                "Privado",
                List.of(day(fixture.exerciseId(), fixture.fillerExerciseId(), structuralType)))).id();
        Routine routine = routineRepository.findById(routineId).orElseThrow();
        routine.setFinishedDate(finishedDate);
        return routineId;
    }

    private RoutineDayInput day(Long targetExerciseId, Long fillerExerciseId, BlockStructuralType structuralType) {
        return new RoutineDayInput(null, null, "Dia 1", null, List.of(
                block("Calentamiento", BlockStructuralType.STANDARD, BlockPurpose.WARMUP, fillerExerciseId, set(10, null)),
                block("Fuerza principal", structuralType, BlockPurpose.MAIN_LIFT, targetExerciseId, set(6, "80")),
                block("Cierre", BlockStructuralType.STANDARD, BlockPurpose.COOLDOWN, fillerExerciseId, set(20, null))
        ));
    }

    private RoutineBlockInput block(String title, BlockStructuralType structuralType, BlockPurpose purpose, Long exerciseId, RoutineExerciseSetInput set) {
        Integer duration = structuralType == BlockStructuralType.CIRCUIT ? 720 : null;
        return new RoutineBlockInput(null, title, structuralType, purpose, duration, null, null, List.of(
                new RoutineExerciseInput(exerciseId, null, null, List.of(set))
        ));
    }

    private RoutineExerciseSetInput set(int reps, String weight) {
        return new RoutineExerciseSetInput(null, SetKind.NORMAL, reps, null, null,
                weight == null ? null : new BigDecimal(weight), null, null, 60, null, null, null, false);
    }

    private Long otherGymId() {
        Long id = ((Number) entityManager.createNativeQuery("SELECT COALESCE(MAX(id), 0) + 1 FROM gyms")
                .getSingleResult()).longValue();
        entityManager.createNativeQuery("""
                        INSERT INTO gyms (id, name, created_at, updated_at)
                        VALUES (:id, :name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """)
                .setParameter("id", id)
                .setParameter("name", "Otro gym " + System.nanoTime())
                .executeUpdate();
        return id;
    }

    private GymPrincipal principal() {
        return new GymPrincipal(1L, "owner@test.local", "password", "Test Owner", UserRole.OWNER, 1L, true);
    }

    private record Fixture(Long studentId, Long exerciseId, Long fillerExerciseId) {
    }
}
