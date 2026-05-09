package com.gymplanner.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymRepository;
import com.gymplanner.shared.exception.ConflictException;
import com.gymplanner.shared.exception.NotFoundException;
import com.gymplanner.student.dto.CreateStudentRequest;
import com.gymplanner.student.dto.UpdateStudentRequest;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StudentServiceTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private GymRepository gymRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createStudentWithValidDataPersists() {
        var response = studentService.create(1L, request("Ana", "Garcia", "123"));

        assertThat(response.id()).isNotNull();
        assertThat(response.firstName()).isEqualTo("Ana");
        assertThat(studentRepository.findById(response.id())).isPresent();
    }

    @Test
    void createStudentWithDuplicatedDocumentInSameGymThrowsConflict() {
        studentService.create(1L, request("Ana", "Garcia", "123"));

        assertThatThrownBy(() -> studentService.create(1L, request("Beto", "Lopez", "123")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createStudentWithDuplicatedDocumentInAnotherGymIsAllowed() {
        Gym otherGym = createOtherGym(99L, "Other Gym");
        studentService.create(1L, request("Ana", "Garcia", "123"));

        var response = studentService.create(otherGym.getId(), request("Beto", "Lopez", "123"));

        assertThat(response.id()).isNotNull();
    }

    @Test
    void searchFindsFirstNameLastNameDocumentAndPhone() {
        studentService.create(1L, request("Ana", "Garcia", "123"));
        studentService.create(1L, new CreateStudentRequest(
                "Luis", "Martinez", "987", "555-444", null, null, null, null, null, null, null));

        assertThat(studentService.list(1L, "Ana", true, null, null, PageRequest.of(0, 10)).content()).hasSize(1);
        assertThat(studentService.list(1L, "Martinez", true, null, null, PageRequest.of(0, 10)).content()).hasSize(1);
        assertThat(studentService.list(1L, "987", true, null, null, PageRequest.of(0, 10)).content()).hasSize(1);
        assertThat(studentService.list(1L, "555-444", true, null, null, PageRequest.of(0, 10)).content()).hasSize(1);
    }

    @Test
    void softDeleteMarksInactive() {
        var response = studentService.create(1L, request("Ana", "Garcia", "123"));

        studentService.deactivate(1L, response.id());

        assertThat(studentRepository.findById(response.id()).orElseThrow().isActive()).isFalse();
    }

    @Test
    void partialUpdateKeepsUnsentFields() {
        var response = studentService.create(1L, request("Ana", "Garcia", "123"));

        var updated = studentService.update(1L, response.id(), new UpdateStudentRequest(
                "Analia", null, null, null, null, null, null, null, null, null, null));

        assertThat(updated.firstName()).isEqualTo("Analia");
        assertThat(updated.lastName()).isEqualTo("Garcia");
        assertThat(updated.documentId()).isEqualTo("123");
    }

    @Test
    void getStudentFromAnotherGymThrowsNotFound() {
        Gym otherGym = createOtherGym(99L, "Other Gym");
        var response = studentService.create(1L, request("Ana", "Garcia", "123"));

        assertThatThrownBy(() -> studentService.get(otherGym.getId(), response.id()))
                .isInstanceOf(NotFoundException.class);
    }

    private CreateStudentRequest request(String firstName, String lastName, String documentId) {
        return new CreateStudentRequest(
                firstName,
                lastName,
                documentId,
                "555",
                "test@example.com",
                LocalDate.of(2000, 1, 1),
                "Futbol",
                "Fuerza",
                "Intermedio",
                "Notas",
                LocalDate.now());
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
