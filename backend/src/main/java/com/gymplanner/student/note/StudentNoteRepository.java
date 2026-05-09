package com.gymplanner.student.note;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentNoteRepository extends JpaRepository<StudentNote, Long> {

    List<StudentNote> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    Optional<StudentNote> findByIdAndStudentId(Long id, Long studentId);
}
