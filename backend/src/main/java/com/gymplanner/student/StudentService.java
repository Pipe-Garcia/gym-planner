package com.gymplanner.student;

import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymRepository;
import com.gymplanner.shared.exception.ConflictException;
import com.gymplanner.shared.exception.NotFoundException;
import com.gymplanner.shared.pagination.PageResponse;
import com.gymplanner.student.dto.CreateStudentRequest;
import com.gymplanner.student.dto.StudentResponse;
import com.gymplanner.student.dto.StudentSummaryResponse;
import com.gymplanner.student.dto.UpdateStudentRequest;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final GymRepository gymRepository;
    private final StudentMapper studentMapper;
    private final StudentFieldNormalizer fieldNormalizer;

    @Transactional(readOnly = true)
    public PageResponse<StudentSummaryResponse> list(
            Long gymId,
            String search,
            Boolean active,
            String sport,
            String level,
            Pageable pageable) {
        Page<Student> page = studentRepository.findAll(specification(gymId, search, active, sport, level), pageable);
        return new PageResponse<>(
                page.map(studentMapper::toSummary).getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional
    public StudentResponse create(Long gymId, CreateStudentRequest request) {
        String documentId = fieldNormalizer.normalizeDni(request.documentId());
        String email = fieldNormalizer.normalizeEmail(request.email());
        validateDocumentAvailable(gymId, documentId, null);
        validateEmailAvailable(gymId, email, null);

        Student student = new Student();
        student.setGym(getGym(gymId));
        student.setFirstName(request.firstName().trim());
        student.setLastName(request.lastName().trim());
        student.setDocumentId(documentId);
        student.setPhone(clean(request.phone()));
        student.setEmail(email);
        student.setBirthDate(request.birthDate());
        student.setSport(clean(request.sport()));
        student.setObjective(clean(request.objective()));
        student.setLevel(clean(request.level()));
        student.setGeneralNotes(clean(request.generalNotes()));
        student.setStartedAt(request.startedAt());
        return studentMapper.toResponse(studentRepository.save(student));
    }

    @Transactional(readOnly = true)
    public StudentResponse get(Long gymId, Long id) {
        return studentMapper.toResponse(getEntity(gymId, id));
    }

    @Transactional
    public StudentResponse update(Long gymId, Long id, UpdateStudentRequest request) {
        Student student = getEntity(gymId, id);
        if (request.documentId() != null) {
            String documentId = fieldNormalizer.normalizeDni(request.documentId());
            validateDocumentAvailable(gymId, documentId, id);
            student.setDocumentId(documentId);
        }
        if (StringUtils.hasText(request.firstName())) {
            student.setFirstName(request.firstName().trim());
        }
        if (StringUtils.hasText(request.lastName())) {
            student.setLastName(request.lastName().trim());
        }
        if (request.phone() != null) {
            student.setPhone(clean(request.phone()));
        }
        if (request.email() != null) {
            String email = fieldNormalizer.normalizeEmail(request.email());
            validateEmailAvailable(gymId, email, id);
            student.setEmail(email);
        }
        if (request.birthDate() != null) {
            student.setBirthDate(request.birthDate());
        }
        if (request.sport() != null) {
            student.setSport(clean(request.sport()));
        }
        if (request.objective() != null) {
            student.setObjective(clean(request.objective()));
        }
        if (request.level() != null) {
            student.setLevel(clean(request.level()));
        }
        if (request.generalNotes() != null) {
            student.setGeneralNotes(clean(request.generalNotes()));
        }
        if (request.startedAt() != null) {
            student.setStartedAt(request.startedAt());
        }
        return studentMapper.toResponse(studentRepository.save(student));
    }

    @Transactional
    public void deactivate(Long gymId, Long id) {
        Student student = getEntity(gymId, id);
        student.setActive(false);
        studentRepository.save(student);
    }

    @Transactional
    public StudentResponse reactivate(Long gymId, Long id) {
        Student student = getEntity(gymId, id);
        student.setActive(true);
        return studentMapper.toResponse(studentRepository.save(student));
    }

    @Transactional(readOnly = true)
    public Student getEntity(Long gymId, Long id) {
        return studentRepository.findByIdAndGymId(id, gymId)
                .orElseThrow(() -> new NotFoundException("Student not found."));
    }

    private Gym getGym(Long gymId) {
        return gymRepository.findById(gymId)
                .orElseThrow(() -> new NotFoundException("Gym not found."));
    }

    private void validateDocumentAvailable(Long gymId, String documentId, Long currentStudentId) {
        if (!StringUtils.hasText(documentId)) {
            return;
        }
        boolean exists = currentStudentId == null
                ? studentRepository.existsByGymIdAndDocumentId(gymId, documentId)
                : studentRepository.existsByGymIdAndDocumentIdAndIdNot(gymId, documentId, currentStudentId);
        if (exists) {
            throw new ConflictException(
                    "Ya existe un alumno con ese DNI.",
                    Map.of("documentId", "Ya existe un alumno con ese DNI."));
        }
    }

    private void validateEmailAvailable(Long gymId, String email, Long currentStudentId) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        boolean exists = currentStudentId == null
                ? studentRepository.existsByGymIdAndEmail(gymId, email)
                : studentRepository.existsByGymIdAndEmailAndIdNot(gymId, email, currentStudentId);
        if (exists) {
            throw new ConflictException(
                    "Ya existe un alumno con ese email.",
                    Map.of("email", "Ya existe un alumno con ese email."));
        }
    }

    private Specification<Student> specification(Long gymId, String search, Boolean active, String sport, String level) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("gym").get("id"), gymId));
            if (active != null) {
                predicates.add(builder.equal(root.get("active"), active));
            }
            if (StringUtils.hasText(sport)) {
                predicates.add(builder.equal(builder.lower(root.get("sport")), sport.trim().toLowerCase()));
            }
            if (StringUtils.hasText(level)) {
                predicates.add(builder.equal(builder.lower(root.get("level")), level.trim().toLowerCase()));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("firstName")), pattern),
                        builder.like(builder.lower(root.get("lastName")), pattern),
                        builder.like(builder.lower(root.get("documentId")), pattern),
                        builder.like(builder.lower(root.get("phone")), pattern)));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
