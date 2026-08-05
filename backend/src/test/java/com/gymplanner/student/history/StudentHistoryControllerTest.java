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
class StudentHistoryControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired StudentService studentService;
    @Autowired ExerciseService exerciseService;
    @Autowired RoutineService routineService;
    @Autowired RoutineRepository routineRepository;
    @Autowired EntityManager entityManager;

    @Test
    void summaryRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/students/{studentId}/history/summary", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void timelineRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/students/{studentId}/history/timeline", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exercisesRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/students/{studentId}/history/exercises", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void occurrencesRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/students/{studentId}/history/exercises/{exerciseId}/occurrences", 1L, 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void summaryReturns200WithJwtAndBasicJson() throws Exception {
        Fixture fixture = fixture();
        createRoutine(fixture, "Resumen", RoutineStatus.ACTIVE, LocalDate.of(2026, 5, 1), null);

        mockMvc.perform(get("/api/students/{studentId}/history/summary", fixture.studentId())
                        .with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(fixture.studentId()))
                .andExpect(jsonPath("$.totalRoutines").value(1))
                .andExpect(jsonPath("$.activeRoutineName").value("Resumen"))
                .andExpect(jsonPath("$.distinctExercisesCount").value(2));
    }

    @Test
    void timelineReturns200WithJwtAndBasicJson() throws Exception {
        Fixture fixture = fixture();
        Long routineId = createRoutine(fixture, "Timeline", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        mockMvc.perform(get("/api/students/{studentId}/history/timeline", fixture.studentId())
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].routineId").value(routineId))
                .andExpect(jsonPath("$.content[0].routineName").value("Timeline"))
                .andExpect(jsonPath("$.content[0].daysCount").value(1));
    }

    @Test
    void exercisesReturns200WithJwtAndBasicJson() throws Exception {
        Fixture fixture = fixture();
        createRoutine(fixture, "Exercises", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        mockMvc.perform(get("/api/students/{studentId}/history/exercises", fixture.studentId())
                        .queryParam("search", "sentadilla")
                        .with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].exerciseId").value(fixture.exerciseId()))
                .andExpect(jsonPath("$.content[0].timesUsed").value(1));
    }

    @Test
    void exercisesReturns200WhenSearchIsOmitted() throws Exception {
        Fixture fixture = fixture();
        createRoutine(fixture, "Exercises", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        mockMvc.perform(get("/api/students/{studentId}/history/exercises", fixture.studentId())
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void exercisesReturns200WhenSearchIsEmptyString() throws Exception {
        Fixture fixture = fixture();
        createRoutine(fixture, "Exercises", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        mockMvc.perform(get("/api/students/{studentId}/history/exercises", fixture.studentId())
                        .queryParam("search", "")
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void occurrencesReturns200WithJwtAndBasicJson() throws Exception {
        Fixture fixture = fixture();
        Long routineId = createRoutine(fixture, "Occurrences", RoutineStatus.FINISHED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        mockMvc.perform(get("/api/students/{studentId}/history/exercises/{exerciseId}/occurrences",
                        fixture.studentId(), fixture.exerciseId())
                        .with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].routineId").value(routineId))
                .andExpect(jsonPath("$.content[0].measurementType").value("REPS_WEIGHT"))
                .andExpect(jsonPath("$.content[0].sets[0].targetWeightKg").value(80.0));
    }

    @Test
    void summaryReturns404WhenStudentBelongsToAnotherGym() throws Exception {
        Long otherStudentId = createStudent(otherGymId());

        mockMvc.perform(get("/api/students/{studentId}/history/summary", otherStudentId)
                        .with(user(principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Alumno no encontrado"));
    }

    @Test
    void timelineReturns404WhenStudentBelongsToAnotherGym() throws Exception {
        Long otherStudentId = createStudent(otherGymId());

        mockMvc.perform(get("/api/students/{studentId}/history/timeline", otherStudentId)
                        .with(user(principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Alumno no encontrado"));
    }

    @Test
    void exercisesReturns404WhenStudentBelongsToAnotherGym() throws Exception {
        Long otherStudentId = createStudent(otherGymId());

        mockMvc.perform(get("/api/students/{studentId}/history/exercises", otherStudentId)
                        .with(user(principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Alumno no encontrado"));
    }

    @Test
    void occurrencesReturns404WhenStudentBelongsToAnotherGym() throws Exception {
        Long otherStudentId = createStudent(otherGymId());
        Long exerciseId = createExercise(1L, MeasurementType.REPS_WEIGHT, "Remo");

        mockMvc.perform(get("/api/students/{studentId}/history/exercises/{exerciseId}/occurrences", otherStudentId, exerciseId)
                        .with(user(principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Alumno no encontrado"));
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

    private Long createRoutine(Fixture fixture, String name, RoutineStatus status, LocalDate assignedDate, LocalDate finishedDate) {
        Long routineId = routineService.createFromScratch(1L, 1L, new CreateRoutineFromScratchRequest(
                fixture.studentId(),
                name,
                "Fuerza",
                status,
                assignedDate,
                null,
                "Privado",
                List.of(day(fixture.exerciseId(), fixture.fillerExerciseId())))).id();
        Routine routine = routineRepository.findById(routineId).orElseThrow();
        routine.setFinishedDate(finishedDate);
        return routineId;
    }

    private RoutineDayInput day(Long targetExerciseId, Long fillerExerciseId) {
        return new RoutineDayInput(null, null, "Dia 1", null, List.of(
                block("Entrada en calor", BlockStructuralType.STANDARD, BlockPurpose.WARMUP, fillerExerciseId, set(10, null)),
                block("Fuerza principal", BlockStructuralType.STANDARD, BlockPurpose.MAIN_LIFT, targetExerciseId, set(6, "80")),
                block("Cierre", BlockStructuralType.STANDARD, BlockPurpose.COOLDOWN, fillerExerciseId, set(20, null))
        ));
    }

    private RoutineBlockInput block(String title, BlockStructuralType structuralType, BlockPurpose purpose, Long exerciseId, RoutineExerciseSetInput set) {
        return new RoutineBlockInput(null, title, structuralType, purpose, null, null, null, List.of(
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
