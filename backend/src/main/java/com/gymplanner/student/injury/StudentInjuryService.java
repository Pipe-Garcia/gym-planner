package com.gymplanner.student.injury;

import com.gymplanner.shared.exception.NotFoundException;
import com.gymplanner.student.Student;
import com.gymplanner.student.StudentService;
import com.gymplanner.student.injury.dto.CreateInjuryRequest;
import com.gymplanner.student.injury.dto.InjuryResponse;
import com.gymplanner.student.injury.dto.UpdateInjuryRequest;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StudentInjuryService {

    private final StudentInjuryRepository injuryRepository;
    private final StudentService studentService;
    private final StudentInjuryMapper injuryMapper;

    @Transactional(readOnly = true)
    public List<InjuryResponse> list(Long gymId, Long studentId, Boolean active) {
        studentService.getEntity(gymId, studentId);
        List<StudentInjury> injuries = active == null
                ? injuryRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
                : injuryRepository.findByStudentIdAndActiveOrderByCreatedAtDesc(studentId, active);
        return injuries.stream().map(injuryMapper::toResponse).toList();
    }

    @Transactional
    public InjuryResponse create(Long gymId, Long studentId, CreateInjuryRequest request) {
        Student student = studentService.getEntity(gymId, studentId);
        StudentInjury injury = new StudentInjury();
        injury.setStudent(student);
        injury.setBodyArea(request.bodyArea().trim());
        injury.setDescription(request.description().trim());
        injury.setSeverity(request.severity());
        injury.setStartedAt(request.startedAt());
        injury.setNotes(clean(request.notes()));
        return injuryMapper.toResponse(injuryRepository.save(injury));
    }

    @Transactional
    public InjuryResponse update(Long gymId, Long studentId, Long injuryId, UpdateInjuryRequest request) {
        studentService.getEntity(gymId, studentId);
        StudentInjury injury = getInjury(studentId, injuryId);
        if (StringUtils.hasText(request.bodyArea())) {
            injury.setBodyArea(request.bodyArea().trim());
        }
        if (StringUtils.hasText(request.description())) {
            injury.setDescription(request.description().trim());
        }
        if (request.severity() != null) {
            injury.setSeverity(request.severity());
        }
        if (request.startedAt() != null) {
            injury.setStartedAt(request.startedAt());
        }
        if (request.resolvedAt() != null) {
            injury.setResolvedAt(request.resolvedAt());
        }
        if (request.active() != null) {
            injury.setActive(request.active());
        }
        if (request.notes() != null) {
            injury.setNotes(clean(request.notes()));
        }
        return injuryMapper.toResponse(injuryRepository.save(injury));
    }

    @Transactional
    public void deactivate(Long gymId, Long studentId, Long injuryId) {
        studentService.getEntity(gymId, studentId);
        StudentInjury injury = getInjury(studentId, injuryId);
        injury.setActive(false);
        if (injury.getResolvedAt() == null) {
            injury.setResolvedAt(LocalDate.now());
        }
        injuryRepository.save(injury);
    }

    private StudentInjury getInjury(Long studentId, Long injuryId) {
        return injuryRepository.findByIdAndStudentId(injuryId, studentId)
                .orElseThrow(() -> new NotFoundException("Student injury not found."));
    }

    private String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
