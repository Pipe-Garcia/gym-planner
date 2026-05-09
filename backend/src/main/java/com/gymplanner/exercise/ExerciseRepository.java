package com.gymplanner.exercise;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ExerciseRepository extends JpaRepository<Exercise, Long>, JpaSpecificationExecutor<Exercise> {

    Optional<Exercise> findByIdAndGymId(Long id, Long gymId);

    boolean existsByGymIdAndSlug(Long gymId, String slug);

    boolean existsByGymIdAndSlugAndIdNot(Long gymId, String slug, Long id);
}
