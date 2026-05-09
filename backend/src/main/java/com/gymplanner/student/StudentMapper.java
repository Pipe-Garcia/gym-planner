package com.gymplanner.student;

import com.gymplanner.student.dto.StudentResponse;
import com.gymplanner.student.dto.StudentSummaryResponse;
import com.gymplanner.student.injury.StudentInjuryMapper;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {

    private final StudentInjuryMapper injuryMapper;

    public StudentMapper(StudentInjuryMapper injuryMapper) {
        this.injuryMapper = injuryMapper;
    }

    public StudentSummaryResponse toSummary(Student student) {
        return new StudentSummaryResponse(
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getDocumentId(),
                student.getPhone(),
                student.getEmail(),
                student.getSport(),
                student.getLevel(),
                student.isActive(),
                student.getStartedAt(),
                student.getCreatedAt(),
                student.getUpdatedAt());
    }

    public StudentResponse toResponse(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getDocumentId(),
                student.getPhone(),
                student.getEmail(),
                student.getBirthDate(),
                student.getSport(),
                student.getObjective(),
                student.getLevel(),
                student.getGeneralNotes(),
                student.isActive(),
                student.getStartedAt(),
                student.getInjuries().stream()
                        .filter(injury -> injury.isActive())
                        .map(injuryMapper::toResponse)
                        .toList(),
                student.getCreatedAt(),
                student.getUpdatedAt());
    }
}
