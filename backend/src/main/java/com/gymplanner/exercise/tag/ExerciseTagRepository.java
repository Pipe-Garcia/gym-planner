package com.gymplanner.exercise.tag;

import com.gymplanner.exercise.tag.dto.TagUsageResponse;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExerciseTagRepository extends JpaRepository<ExerciseTag, Long> {

    List<ExerciseTag> findByGymIdOrderByTypeAscNameAsc(Long gymId);

    List<ExerciseTag> findByGymIdAndTypeOrderByNameAsc(Long gymId, TagType type);

    List<ExerciseTag> findByGymIdAndIdIn(Long gymId, Collection<Long> ids);

    Optional<ExerciseTag> findByIdAndGymId(Long id, Long gymId);

    boolean existsByGymIdAndTypeAndSlug(Long gymId, TagType type, String slug);

    boolean existsByGymIdAndTypeAndSlugAndIdNot(Long gymId, TagType type, String slug, Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM exercise_tags WHERE id = :id AND gym_id = :gymId", nativeQuery = true)
    int deleteOwnedTag(@Param("id") Long id, @Param("gymId") Long gymId);

    @Query("""
            SELECT new com.gymplanner.exercise.tag.dto.TagUsageResponse(
                tag.id,
                tag.type,
                tag.name,
                tag.slug,
                (
                    SELECT COUNT(exercise.id)
                    FROM Exercise exercise
                    JOIN exercise.tags assignedTag
                    WHERE assignedTag.id = tag.id
                      AND exercise.gym.id = :gymId
                )
            )
            FROM ExerciseTag tag
            WHERE tag.gym.id = :gymId
              AND (:type IS NULL OR tag.type = :type)
            ORDER BY tag.type ASC, tag.name ASC
            """)
    List<TagUsageResponse> findUsageByGymIdAndOptionalType(
            @Param("gymId") Long gymId,
            @Param("type") TagType type);
}
