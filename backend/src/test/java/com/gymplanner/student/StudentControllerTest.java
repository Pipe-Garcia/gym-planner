package com.gymplanner.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymplanner.auth.CustomUserDetailsService.GymPrincipal;
import com.gymplanner.user.UserRole;
import jakarta.persistence.EntityManager;
import java.util.concurrent.atomic.AtomicLong;
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
class StudentControllerTest {

    private static final AtomicLong OTHER_GYM_ID = new AtomicLong(100);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void postValidStudentReturnsCreated() throws Exception {
        mockMvc.perform(post("/api/students")
                        .with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ana","lastName":"Garcia","documentId":"123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Ana"));
    }

    @Test
    void postWithoutFirstNameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/students")
                        .with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lastName":"Garcia","documentId":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.firstName").exists());
    }

    @Test
    void postDuplicatedNormalizedDocumentReturnsFieldConflict() throws Exception {
        mockMvc.perform(post("/api/students")
                        .with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ana","lastName":"Garcia","documentId":"12.345.678"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/students")
                        .with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Beto","lastName":"Lopez","documentId":"12345678"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.documentId")
                        .value("Ya existe un alumno con ese DNI."));
    }

    @Test
    void postDuplicatedNormalizedEmailReturnsFieldConflict() throws Exception {
        mockMvc.perform(post("/api/students")
                        .with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ana","lastName":"Garcia","email":" A@x.com "}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/students")
                        .with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Beto","lastName":"Lopez","email":"a@x.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.email")
                        .value("Ya existe un alumno con ese email."));
    }

    @Test
    void checkPhoneReturnsOnlyMatchStatusAndStudentName() throws Exception {
        mockMvc.perform(post("/api/students")
                        .with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"Ana",
                                  "lastName":"Garcia",
                                  "documentId":"123",
                                  "phone":"011 15-2345-6789",
                                  "email":"ana@example.com"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/students/check-phone")
                        .with(user(principal()))
                        .param("phone", "+54 9 11 2345-6789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.studentName").value("Ana Garcia"))
                .andExpect(jsonPath("$.documentId").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void getStudentsWithoutAuthReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getStudentsWithActiveTrueReturnsOnlyActiveStudentsFromPrincipalGym() throws Exception {
        Long otherGymId = createOtherGym("Other Active Gym");
        createStudent(principal(), "Active", "Student", "1001");
        Long inactiveStudentId = createStudent(principal(), "Inactive", "Student", "1002");
        deactivateStudent(principal(), inactiveStudentId);
        createStudent(principal(otherGymId), "Other", "Active", "2001");
        Long otherInactiveStudentId = createStudent(principal(otherGymId), "Other", "Inactive", "2002");
        deactivateStudent(principal(otherGymId), otherInactiveStudentId);

        mockMvc.perform(get("/api/students")
                        .with(user(principal()))
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].documentId").value("1001"))
                .andExpect(jsonPath("$.content[0].active").value(true));
    }

    @Test
    void getStudentsWithActiveFalseReturnsOnlyInactiveStudentsFromPrincipalGym() throws Exception {
        Long otherGymId = createOtherGym("Other Inactive Gym");
        createStudent(principal(), "Active", "Student", "1101");
        Long inactiveStudentId = createStudent(principal(), "Inactive", "Student", "1102");
        deactivateStudent(principal(), inactiveStudentId);
        createStudent(principal(otherGymId), "Other", "Active", "2101");
        Long otherInactiveStudentId = createStudent(principal(otherGymId), "Other", "Inactive", "2102");
        deactivateStudent(principal(otherGymId), otherInactiveStudentId);

        mockMvc.perform(get("/api/students")
                        .with(user(principal()))
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].documentId").value("1102"))
                .andExpect(jsonPath("$.content[0].active").value(false));
    }

    @Test
    void getStudentsWithoutActiveParamReturnsAllStudentsFromPrincipalGym() throws Exception {
        Long otherGymId = createOtherGym("Other All Gym");
        createStudent(principal(), "Active", "Student", "1201");
        Long inactiveStudentId = createStudent(principal(), "Inactive", "Student", "1202");
        deactivateStudent(principal(), inactiveStudentId);
        createStudent(principal(otherGymId), "Other", "Active", "2201");
        Long otherInactiveStudentId = createStudent(principal(otherGymId), "Other", "Inactive", "2202");
        deactivateStudent(principal(otherGymId), otherInactiveStudentId);

        mockMvc.perform(get("/api/students").with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].documentId", containsInAnyOrder("1201", "1202")));
    }

    @Test
    void deleteStudentReturnsNoContentAndMarksInactive() throws Exception {
        String location = mockMvc.perform(post("/api/students")
                        .with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ana","lastName":"Garcia","documentId":"123"}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long id = Long.valueOf(location.replaceAll(".*\"id\":(\\d+).*", "$1"));

        mockMvc.perform(delete("/api/students/{id}", id).with(user(principal())))
                .andExpect(status().isNoContent());

        assertThat(studentRepository.findById(id).orElseThrow().isActive()).isFalse();
    }

    private GymPrincipal principal() {
        return principal(1L);
    }

    private GymPrincipal principal(Long gymId) {
        return new GymPrincipal(1L, "owner@test.local", "password", "Test Owner", UserRole.OWNER, gymId, true);
    }

    private Long createOtherGym(String name) {
        Long id = OTHER_GYM_ID.getAndIncrement();
        entityManager.createNativeQuery("INSERT INTO gyms (id, name) VALUES (?, ?)")
                .setParameter(1, id)
                .setParameter(2, name)
                .executeUpdate();
        entityManager.flush();
        return id;
    }

    private Long createStudent(GymPrincipal principal, String firstName, String lastName, String documentId) throws Exception {
        String response = mockMvc.perform(post("/api/students")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"%s","lastName":"%s","documentId":"%s"}
                                """.formatted(firstName, lastName, documentId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return Long.valueOf(response.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    private void deactivateStudent(GymPrincipal principal, Long id) throws Exception {
        mockMvc.perform(delete("/api/students/{id}", id).with(user(principal)))
                .andExpect(status().isNoContent());
    }
}
