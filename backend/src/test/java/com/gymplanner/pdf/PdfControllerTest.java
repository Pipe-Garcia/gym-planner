package com.gymplanner.pdf;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymplanner.auth.CustomUserDetailsService.GymPrincipal;
import com.gymplanner.exercise.ExerciseService;
import com.gymplanner.exercise.MeasurementType;
import com.gymplanner.exercise.dto.CreateExerciseRequest;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PdfControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired RoutineService routineService;
    @Autowired StudentService studentService;
    @Autowired ExerciseService exerciseService;

    @Test
    void getPdf_returns200WithApplicationPdf() throws Exception {
        Long routineId = fixture();

        mockMvc.perform(get("/api/routines/{id}/pdf", routineId).with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment; filename=")));
    }

    @Test
    void getPdf_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/routines/{id}/pdf", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getText_returns200WithTextPlain() throws Exception {
        Long routineId = fixture();

        mockMvc.perform(get("/api/routines/{id}/text", routineId).with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("text/plain")));
    }

    @Test
    void getPdf_usesStudentSlugInFilename() throws Exception {
        Long routineId = fixture("Lucia", "Salcedo");

        mockMvc.perform(get("/api/routines/{id}/pdf", routineId).with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rutina_lucia_salcedo_2026-05-12.pdf\""));
    }

    private Long fixture() {
        return fixture("Martin", "Gomez");
    }

    private Long fixture(String firstName, String lastName) {
        Long studentId = studentService.create(1L, new CreateStudentRequest(
                firstName,
                lastName,
                null,
                "555",
                null,
                null,
                "Voley",
                "Saltabilidad",
                "Intermedio",
                null,
                LocalDate.now())).id();
        Long exerciseId = exerciseService.create(1L, new CreateExerciseRequest(
                "Press Controller " + System.nanoTime(),
                "Desc",
                null,
                MeasurementType.REPS_WEIGHT,
                null,
                null,
                List.of())).id();
        return routineService.createFromScratch(1L, 1L, new CreateRoutineFromScratchRequest(
                studentId,
                "Plantilla Voley",
                "Saltabilidad",
                RoutineStatus.ACTIVE,
                LocalDate.of(2026, 5, 12),
                "Tomar agua entre series",
                "Nota privada",
                List.of(day(exerciseId)))).id();
    }

    private RoutineDayInput day(Long exerciseId) {
        return new RoutineDayInput(null, null, "Zona media", null, List.of(
                block("Movilidad", BlockPurpose.WARMUP, exerciseId),
                block("Fuerza", BlockPurpose.MAIN_LIFT, exerciseId),
                block("Vuelta", BlockPurpose.COOLDOWN, exerciseId)
        ));
    }

    private RoutineBlockInput block(String title, BlockPurpose purpose, Long exerciseId) {
        return new RoutineBlockInput(null, title, BlockStructuralType.STANDARD, purpose, null, null, null, List.of(
                new RoutineExerciseInput(exerciseId, null, null, List.of(
                        new RoutineExerciseSetInput(null, SetKind.NORMAL, 10, null, null, new BigDecimal("20"), null, null, 60, null, null, null, false)
                ))
        ));
    }

    private GymPrincipal principal() {
        return new GymPrincipal(1L, "admin@gymplanner.local", "password", "Owner Demo", UserRole.OWNER, 1L, true);
    }
}
