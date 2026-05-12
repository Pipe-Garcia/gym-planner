package com.gymplanner.template;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainingTemplateRepository extends JpaRepository<TrainingTemplate, Long>, JpaSpecificationExecutor<TrainingTemplate> {
    Optional<TrainingTemplate> findByIdAndGymId(Long id, Long gymId);

    @Query("""
            SELECT DISTINCT t FROM TrainingTemplate t
            LEFT JOIN FETCH t.days d
            LEFT JOIN FETCH d.blocks b
            LEFT JOIN FETCH b.exercises e
            LEFT JOIN FETCH e.sets s
            LEFT JOIN FETCH e.exercise ex
            LEFT JOIN FETCH ex.tags tags
            WHERE t.id = :id
            """)
    Optional<TrainingTemplate> findByIdWithFullStructure(@Param("id") Long id);

    @Query("SELECT COUNT(d) FROM TemplateDay d WHERE d.template.id = :templateId")
    long countDays(@Param("templateId") Long templateId);

    @Query("SELECT COUNT(b) FROM TemplateBlock b WHERE b.day.template.id = :templateId")
    long countBlocks(@Param("templateId") Long templateId);

    @Query("SELECT COUNT(e) FROM TemplateExercise e WHERE e.block.day.template.id = :templateId")
    long countExercises(@Param("templateId") Long templateId);
}
