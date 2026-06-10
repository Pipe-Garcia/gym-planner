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
        var response = studentService.create(1L, request("Ana", "Garcia", "12.345.678"));

        assertThat(response.id()).isNotNull();
        assertThat(response.firstName()).isEqualTo("Ana");
        assertThat(response.documentId()).isEqualTo("12345678");
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(studentRepository.findById(response.id())).isPresent();
    }

    @Test
    void createStudentWithEquivalentNormalizedDocumentInSameGymThrowsFieldConflict() {
        studentService.create(1L, request("Ana", "Garcia", "12.345.678"));

        assertThatThrownBy(() -> studentService.create(1L, request("Beto", "Lopez", "12345678")))
                .isInstanceOfSatisfying(ConflictException.class, exception ->
                        assertThat(exception.fieldErrors())
                                .containsEntry("documentId", "Ya existe un alumno con ese DNI."));
    }

    @Test
    void createStudentWithEquivalentNormalizedEmailInSameGymThrowsFieldConflict() {
        studentService.create(1L, request("Ana", "Garcia", "123", " A@x.com "));

        assertThatThrownBy(() -> studentService.create(1L, request("Beto", "Lopez", "456", "a@x.com")))
                .isInstanceOfSatisfying(ConflictException.class, exception ->
                        assertThat(exception.fieldErrors())
                                .containsEntry("email", "Ya existe un alumno con ese email."));
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
    void updateStudentKeepingOwnNormalizedDocumentAndEmailDoesNotConflict() {
        var response = studentService.create(1L, request("Ana", "Garcia", "12.345.678", " ANA@EXAMPLE.COM "));

        var updated = studentService.update(1L, response.id(), new UpdateStudentRequest(
                null, null, "12 345 678", null, " ana@example.com ", null, null, null, null, null, null));

        assertThat(updated.documentId()).isEqualTo("12345678");
        assertThat(updated.email()).isEqualTo("ana@example.com");
    }

    @Test
    void createMultipleStudentsWithoutDocumentOrEmailIsAllowed() {
        var first = studentService.create(1L, request("Ana", "Garcia", "", " "));
        var second = studentService.create(1L, request("Beto", "Lopez", "---", ""));

        assertThat(first.documentId()).isNull();
        assertThat(first.email()).isNull();
        assertThat(second.documentId()).isNull();
        assertThat(second.email()).isNull();
    }

    @Test
    void checkPhoneReturnsStudentNameForNormalizedMatchInSameGym() {
        studentService.create(1L, phoneRequest("Ana", "Garcia", "101", "011 15-2345-6789"));

        var response = studentService.checkPhone(1L, "+54 9 11 2345-6789", null);

        assertThat(response.exists()).isTrue();
        assertThat(response.studentName()).isEqualTo("Ana Garcia");
    }

    @Test
    void checkPhoneReturnsEmptyWhenThereIsNoMatch() {
        studentService.create(1L, phoneRequest("Ana", "Garcia", "101", "011 15-2345-6789"));

        var response = studentService.checkPhone(1L, "351 555-6789", null);

        assertThat(response.exists()).isFalse();
        assertThat(response.studentName()).isNull();
    }

    @Test
    void checkPhoneExcludesCurrentStudentWhenEditing() {
        var student = studentService.create(
                1L,
                phoneRequest("Ana", "Garcia", "101", "011 15-2345-6789"));

        var response = studentService.checkPhone(1L, "5491123456789", student.id());

        assertThat(response.exists()).isFalse();
        assertThat(response.studentName()).isNull();
    }

    @Test
    void checkPhoneDoesNotCrossGyms() {
        Gym otherGym = createOtherGym(99L, "Other Gym");
        studentService.create(
                otherGym.getId(),
                phoneRequest("Ana", "Garcia", "101", "011 15-2345-6789"));

        var response = studentService.checkPhone(1L, "+54 9 11 2345-6789", null);

        assertThat(response.exists()).isFalse();
        assertThat(response.studentName()).isNull();
    }

    @Test
    void getStudentFromAnotherGymThrowsNotFound() {
        Gym otherGym = createOtherGym(99L, "Other Gym");
        var response = studentService.create(1L, request("Ana", "Garcia", "123"));

        assertThatThrownBy(() -> studentService.get(otherGym.getId(), response.id()))
                .isInstanceOf(NotFoundException.class);
    }

    private CreateStudentRequest request(String firstName, String lastName, String documentId) {
        return request(firstName, lastName, documentId, "test@example.com");
    }

    private CreateStudentRequest request(String firstName, String lastName, String documentId, String email) {
        return new CreateStudentRequest(
                firstName,
                lastName,
                documentId,
                "555",
                email,
                LocalDate.of(2000, 1, 1),
                "Futbol",
                "Fuerza",
                "Intermedio",
                "Notas",
                LocalDate.now());
    }

    private CreateStudentRequest phoneRequest(String firstName, String lastName, String documentId, String phone) {
        return new CreateStudentRequest(
                firstName,
                lastName,
                documentId,
                phone,
                null,
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
