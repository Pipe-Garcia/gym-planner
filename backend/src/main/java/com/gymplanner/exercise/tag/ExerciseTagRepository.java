package com.gymplanner.exercise.tag;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseTagRepository extends JpaRepository<ExerciseTag, Long> {

    List<ExerciseTag> findByGymIdOrderByTypeAscNameAsc(Long gymId);

    List<ExerciseTag> findByGymIdAndTypeOrderByNameAsc(Long gymId, TagType type);

    List<ExerciseTag> findByGymIdAndIdIn(Long gymId, Collection<Long> ids);
}
