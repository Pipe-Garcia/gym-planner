package com.gymplanner.student;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymRepository;
import com.gymplanner.shared.exception.NotFoundException;
import com.gymplanner.student.dto.CreateStudentRequest;
import com.gymplanner.student.dto.StudentResponse;
import com.gymplanner.student.injury.InjurySeverity;
import com.gymplanner.student.injury.StudentInjuryService;
import com.gymplanner.student.injury.dto.CreateInjuryRequest;
import com.gymplanner.student.injury.dto.InjuryResponse;
import com.gymplanner.student.injury.dto.UpdateInjuryRequest;
import com.gymplanner.student.note.StudentNoteService;
import com.gymplanner.student.note.dto.CreateNoteRequest;
import com.gymplanner.student.note.dto.NoteResponse;
import com.gymplanner.support.PostgresIntegrationTest;
import com.gymplanner.user.User;
import com.gymplanner.user.UserRepository;
import com.gymplanner.user.UserRole;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class StudentInjuryNoteCrossTenantIntegrationTest extends PostgresIntegrationTest {

    private static final Long GYM_A_ID = 1L;
    private static final Long USER_A_ID = 1L;

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentInjuryService injuryService;

    @Autowired
    private StudentNoteService noteService;

    @Autowired
    private GymRepository gymRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void injuryListDoesNotExposeStudentFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Injury List");
        StudentResponse otherStudent = createStudent(otherGym.getId(), "Other", "InjuryList", "b-injury-list");

        assertThatThrownBy(() -> injuryService.list(GYM_A_ID, otherStudent.id(), null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void injuryCreateDoesNotAttachToStudentFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Injury Create");
        StudentResponse otherStudent = createStudent(otherGym.getId(), "Other", "InjuryCreate", "b-injury-create");

        assertThatThrownBy(() -> injuryService.create(GYM_A_ID, otherStudent.id(), createInjuryRequest()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void injuryUpdateDoesNotModifyStudentFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Injury Update");
        StudentResponse otherStudent = createStudent(otherGym.getId(), "Other", "InjuryUpdate", "b-injury-update");
        InjuryResponse otherInjury = injuryService.create(otherGym.getId(), otherStudent.id(), createInjuryRequest());

        assertThatThrownBy(() -> injuryService.update(GYM_A_ID, otherStudent.id(), otherInjury.id(), updateInjuryRequest()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void injuryDeactivateDoesNotModifyStudentFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Injury Deactivate");
        StudentResponse otherStudent = createStudent(otherGym.getId(), "Other", "InjuryDeactivate", "b-injury-deactivate");
        InjuryResponse otherInjury = injuryService.create(otherGym.getId(), otherStudent.id(), createInjuryRequest());

        assertThatThrownBy(() -> injuryService.deactivate(GYM_A_ID, otherStudent.id(), otherInjury.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void noteListDoesNotExposeStudentFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Note List");
        StudentResponse otherStudent = createStudent(otherGym.getId(), "Other", "NoteList", "b-note-list");

        assertThatThrownBy(() -> noteService.list(GYM_A_ID, otherStudent.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void noteCreateDoesNotAttachToStudentFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Note Create");
        StudentResponse otherStudent = createStudent(otherGym.getId(), "Other", "NoteCreate", "b-note-create");

        assertThatThrownBy(() -> noteService.create(GYM_A_ID, otherStudent.id(), USER_A_ID, createNoteRequest()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void noteDeleteDoesNotRemoveNoteFromStudentInAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Note Delete");
        User otherUser = createUser(otherGym, "note-delete-other@gymplanner.local");
        StudentResponse otherStudent = createStudent(otherGym.getId(), "Other", "NoteDelete", "b-note-delete");
        NoteResponse otherNote = noteService.create(otherGym.getId(), otherStudent.id(), otherUser.getId(), createNoteRequest());

        assertThatThrownBy(() -> noteService.delete(GYM_A_ID, otherStudent.id(), otherNote.id(), USER_A_ID, UserRole.OWNER))
                .isExactlyInstanceOf(NotFoundException.class);
    }

    @Test
    void injuryUpdateRejectsNestedResourceFromAnotherGymEvenWithOwnStudentId() {
        Gym otherGym = createOtherGym("Other Gym Nested Injury Update");
        StudentResponse ownStudent = createStudent(GYM_A_ID, "Own", "NestedInjuryUpdate", "a-nested-injury-update");
        StudentResponse otherStudent = createStudent(otherGym.getId(), "Other", "NestedInjuryUpdate", "b-nested-injury-update");
        InjuryResponse otherInjury = injuryService.create(otherGym.getId(), otherStudent.id(), createInjuryRequest());

        assertThatThrownBy(() -> injuryService.update(GYM_A_ID, ownStudent.id(), otherInjury.id(), updateInjuryRequest()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void injuryDeactivateRejectsNestedResourceFromAnotherGymEvenWithOwnStudentId() {
        Gym otherGym = createOtherGym("Other Gym Nested Injury Deactivate");
        StudentResponse ownStudent = createStudent(GYM_A_ID, "Own", "NestedInjuryDeactivate", "a-nested-injury-deactivate");
        StudentResponse otherStudent = createStudent(otherGym.getId(), "Other", "NestedInjuryDeactivate", "b-nested-injury-deactivate");
        InjuryResponse otherInjury = injuryService.create(otherGym.getId(), otherStudent.id(), createInjuryRequest());

        assertThatThrownBy(() -> injuryService.deactivate(GYM_A_ID, ownStudent.id(), otherInjury.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void noteDeleteRejectsNestedResourceFromAnotherGymEvenWithOwnStudentId() {
        Gym otherGym = createOtherGym("Other Gym Nested Note Delete");
        User otherUser = createUser(otherGym, "nested-note-other@gymplanner.local");
        StudentResponse ownStudent = createStudent(GYM_A_ID, "Own", "NestedNoteDelete", "a-nested-note-delete");
        StudentResponse otherStudent = createStudent(otherGym.getId(), "Other", "NestedNoteDelete", "b-nested-note-delete");
        NoteResponse otherNote = noteService.create(otherGym.getId(), otherStudent.id(), otherUser.getId(), createNoteRequest());

        assertThatThrownBy(() -> noteService.delete(GYM_A_ID, ownStudent.id(), otherNote.id(), USER_A_ID, UserRole.OWNER))
                .isExactlyInstanceOf(NotFoundException.class);
    }

    @Test
    void noteCreateRejectsAuthorFromAnotherGym() {
        Gym otherGym = createOtherGym("Other Gym Author");
        User otherUser = createUser(otherGym, "author-other@gymplanner.local");
        StudentResponse ownStudent = createStudent(GYM_A_ID, "Own", "Author", "a-author");

        assertThatThrownBy(() -> noteService.create(GYM_A_ID, ownStudent.id(), otherUser.getId(), createNoteRequest()))
                .isInstanceOf(NotFoundException.class);
    }

    private Gym createOtherGym(String name) {
        Gym gym = new Gym();
        gym.setName(name);
        return gymRepository.save(gym);
    }

    private User createUser(Gym gym, String email) {
        User user = new User();
        user.setGym(gym);
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setFullName("Other Owner");
        user.setRole(UserRole.OWNER);
        user.setActive(true);
        return userRepository.save(user);
    }

    private StudentResponse createStudent(Long gymId, String firstName, String lastName, String documentId) {
        return studentService.create(gymId, createStudentRequest(firstName, lastName, documentId));
    }

    private CreateStudentRequest createStudentRequest(String firstName, String lastName, String documentId) {
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

    private CreateInjuryRequest createInjuryRequest() {
        return new CreateInjuryRequest(
                "Rodilla",
                "Molestia leve",
                InjurySeverity.LEVE,
                LocalDate.now(),
                "Cuidar carga");
    }

    private UpdateInjuryRequest updateInjuryRequest() {
        return new UpdateInjuryRequest(
                "Hombro",
                "Actualizada",
                InjurySeverity.MODERADA,
                LocalDate.now(),
                null,
                true,
                "Seguimiento");
    }

    private CreateNoteRequest createNoteRequest() {
        return new CreateNoteRequest("Nota privada");
    }
}
