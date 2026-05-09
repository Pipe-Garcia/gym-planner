package com.gymplanner.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateStudentRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 50) String documentId,
        @Size(max = 50) String phone,
        @Email @Size(max = 150) String email,
        @Past LocalDate birthDate,
        @Size(max = 100) String sport,
        @Size(max = 150) String objective,
        @Size(max = 50) String level,
        String generalNotes,
        @PastOrPresent LocalDate startedAt) {
}
