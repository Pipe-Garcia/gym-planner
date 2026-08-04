package com.gymplanner.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymplanner.auth.CustomUserDetailsService.GymPrincipal;
import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymRepository;
import com.gymplanner.student.dto.CreateStudentRequest;
import com.gymplanner.student.dto.StudentResponse;
import com.gymplanner.user.UserRole;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
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
class StudentControllerCrossTenantTest {

    private static final Long GYM_A_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentService studentService;

    @Autowired
    private GymRepository gymRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void getStudentFromAnotherGymReturnsNotFoundAndKeepsStudent() throws Exception {
        Gym otherGym = createOtherGym();
        StudentResponse otherStudent = createStudent(otherGym.getId(), "Other");

        mockMvc.perform(get("/api/students/{id}", otherStudent.id()).with(user(principal())))
                .andExpect(status().isNotFound());

        assertThat(studentService.get(otherGym.getId(), otherStudent.id()))
                .extracting(StudentResponse::id)
                .isEqualTo(otherStudent.id());
    }

    @Test
    void getStudentFromPrincipalGymReturnsOk() throws Exception {
        StudentResponse ownStudent = createStudent(GYM_A_ID, "Own");

        mockMvc.perform(get("/api/students/{id}", ownStudent.id()).with(user(principal())))
                .andExpect(status().isOk());
    }

    private StudentResponse createStudent(Long gymId, String label) {
        String suffix = uniqueSuffix();
        return studentService.create(gymId, new CreateStudentRequest(
                label,
                "HTTP " + suffix,
                "http-student-" + suffix,
                null,
                null,
                LocalDate.of(2000, 1, 1),
                "Voley",
                "Saltabilidad",
                "Intermedio",
                null,
                LocalDate.now()));
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

    private String uniqueSuffix() {
        return Long.toString(System.nanoTime());
    }

    private GymPrincipal principal() {
        return new GymPrincipal(1L, "owner@test.local", "password", "Test Owner", UserRole.OWNER, 1L, true);
    }
}
