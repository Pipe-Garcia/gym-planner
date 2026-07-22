package com.gymplanner.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymRepository;
import com.gymplanner.shared.exception.NotFoundException;
import com.gymplanner.student.dto.CreateStudentRequest;
import com.gymplanner.student.dto.StudentResponse;
import com.gymplanner.student.dto.StudentSummaryResponse;
import com.gymplanner.student.dto.UpdateStudentRequest;
import com.gymplanner.support.PostgresIntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class StudentCrossTenantIntegrationTest extends PostgresIntegrationTest {

    private static final Long GYM_A_ID = 1L;

    @Autowired
    private StudentService studentService;

    @Autowired
    private GymRepository gymRepository;

    @Test
    void getDoesNotExposeStudentFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Get");
        StudentResponse otherStudent = createStudent(otherGym.getId(), "Other", "Get", "b-get");

        assertThatThrownBy(() -> studentService.get(GYM_A_ID, otherStudent.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateDoesNotModifyStudentFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Update");
        StudentResponse otherStudent = createStudent(otherGym.getId(), "Other", "Update", "b-update");

        assertThatThrownBy(() -> studentService.update(GYM_A_ID, otherStudent.id(), updateRequest("Changed")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deactivateDoesNotAffectStudentFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Deactivate");
        StudentResponse otherStudent = createStudent(otherGym.getId(), "Other", "Deactivate", "b-deactivate");

        assertThatThrownBy(() -> studentService.deactivate(GYM_A_ID, otherStudent.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void reactivateDoesNotAffectStudentFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Reactivate");
        StudentResponse otherStudent = createStudent(otherGym.getId(), "Other", "Reactivate", "b-reactivate");
        studentService.deactivate(otherGym.getId(), otherStudent.id());

        assertThatThrownBy(() -> studentService.reactivate(GYM_A_ID, otherStudent.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getEntityDoesNotExposeStudentFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Entity");
        StudentResponse otherStudent = createStudent(otherGym.getId(), "Other", "Entity", "b-entity");

        assertThatThrownBy(() -> studentService.getEntity(GYM_A_ID, otherStudent.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listDoesNotIncludeStudentsFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym List");
        StudentResponse ownStudent = createStudent(GYM_A_ID, "Own", "List", "a-list");
        StudentResponse otherStudent = createStudent(otherGym.getId(), "Other", "List", "b-list");

        var result = studentService.list(GYM_A_ID, "List", true, null, null, PageRequest.of(0, 20));

        assertThat(result.content())
                .extracting(StudentSummaryResponse::id)
                .contains(ownStudent.id())
                .doesNotContain(otherStudent.id());
    }

    private Gym createOtherGym(String name) {
        Gym gym = new Gym();
        gym.setName(name);
        return gymRepository.save(gym);
    }

    private StudentResponse createStudent(Long gymId, String firstName, String lastName, String documentId) {
        return studentService.create(gymId, createRequest(firstName, lastName, documentId));
    }

    private CreateStudentRequest createRequest(String firstName, String lastName, String documentId) {
        return new CreateStudentRequest(
                firstName,
                lastName,
                documentId,
                null,
                null,
                LocalDate.of(2000, 1, 1),
                "Futbol",
                "Fuerza",
                "Intermedio",
                null,
                LocalDate.now());
    }

    private UpdateStudentRequest updateRequest(String firstName) {
        return new UpdateStudentRequest(
                firstName,
                "Updated",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
