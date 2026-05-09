package com.gymplanner.student.injury;

import com.gymplanner.student.injury.dto.InjuryResponse;
import org.springframework.stereotype.Component;

@Component
public class StudentInjuryMapper {

    public InjuryResponse toResponse(StudentInjury injury) {
        return new InjuryResponse(
                injury.getId(),
                injury.getBodyArea(),
                injury.getDescription(),
                injury.getSeverity(),
                injury.getStartedAt(),
                injury.getResolvedAt(),
                injury.isActive(),
                injury.getNotes(),
                injury.getCreatedAt(),
                injury.getUpdatedAt());
    }
}
