package com.gymplanner.student.injury;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentInjuryRepository extends JpaRepository<StudentInjury, Long> {

    List<StudentInjury> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<StudentInjury> findByStudentIdAndActiveOrderByCreatedAtDesc(Long studentId, boolean active);

    Optional<StudentInjury> findByIdAndStudentId(Long id, Long studentId);
}
