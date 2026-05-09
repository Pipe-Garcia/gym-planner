package com.gymplanner.student;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    Optional<Student> findByIdAndGymId(Long id, Long gymId);

    boolean existsByGymIdAndDocumentId(Long gymId, String documentId);

    boolean existsByGymIdAndDocumentIdAndIdNot(Long gymId, String documentId, Long id);
}
