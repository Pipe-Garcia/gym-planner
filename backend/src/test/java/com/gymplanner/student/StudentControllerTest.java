package com.gymplanner.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymplanner.auth.CustomUserDetailsService.GymPrincipal;
import com.gymplanner.user.UserRole;
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

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
        return new GymPrincipal(1L, "admin@gymplanner.local", "password", "Owner Demo", UserRole.OWNER, 1L, true);
    }
}
