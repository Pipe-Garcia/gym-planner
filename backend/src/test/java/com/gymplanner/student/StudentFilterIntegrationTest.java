package com.gymplanner.student;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymRepository;
import com.gymplanner.student.dto.CreateStudentRequest;
import com.gymplanner.support.PostgresIntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class StudentFilterIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private GymRepository gymRepository;

    @Test
    void sportFilterMatchesWithoutAccents() {
        studentService.create(1L, request("Ana", "Futbol", "1001", "Fútbol", "Intermedio"));

        var result = studentService.list(1L, null, true, "Futbol", null, PageRequest.of(0, 10));

        assertThat(result.content())
                .singleElement()
                .satisfies(student -> assertThat(student.sport()).isEqualTo("Fútbol"));
    }

    @Test
    void sportFilterMatchesPartialValue() {
        studentService.create(1L, request("Ana", "Partial", "1002", "Futbol", "Intermedio"));

        var result = studentService.list(1L, null, true, "Fut", null, PageRequest.of(0, 10));

        assertThat(result.content())
                .singleElement()
                .satisfies(student -> assertThat(student.sport()).isEqualTo("Futbol"));
    }

    @Test
    void sportFilterMatchesCaseInsensitivePartialValue() {
        studentService.create(1L, request("Ana", "Case", "1003", "Fútbol", "Intermedio"));

        var result = studentService.list(1L, null, true, "fUt", null, PageRequest.of(0, 10));

        assertThat(result.content())
                .singleElement()
                .satisfies(student -> assertThat(student.sport()).isEqualTo("Fútbol"));
    }

    @Test
    void levelFilterMatchesWithoutAccents() {
        studentService.create(1L, request("Ana", "Level", "1004", "Futbol", "Iniciación"));

        var result = studentService.list(1L, null, true, null, "Iniciacion", PageRequest.of(0, 10));

        assertThat(result.content())
                .singleElement()
                .satisfies(student -> assertThat(student.level()).isEqualTo("Iniciación"));
    }

    @Test
    void sportFilterKeepsGymIsolation() {
        Gym otherGym = createOtherGym("Other Gym");
        var currentGymStudent = studentService.create(1L, request("Ana", "Own", "1005", "Fútbol", "Intermedio"));
        studentService.create(otherGym.getId(), request("Beto", "Other", "1005", "Fútbol", "Intermedio"));

        var result = studentService.list(1L, null, true, "Futbol", null, PageRequest.of(0, 10));

        assertThat(result.content())
                .singleElement()
                .satisfies(student -> assertThat(student.id()).isEqualTo(currentGymStudent.id()));
    }

    private Gym createOtherGym(String name) {
        Gym gym = new Gym();
        gym.setName(name);
        return gymRepository.save(gym);
    }

    private CreateStudentRequest request(String firstName, String lastName, String documentId, String sport, String level) {
        return new CreateStudentRequest(
                firstName,
                lastName,
                documentId,
                null,
                null,
                LocalDate.of(2000, 1, 1),
                sport,
                "Fuerza",
                level,
                null,
                LocalDate.now());
    }
}
