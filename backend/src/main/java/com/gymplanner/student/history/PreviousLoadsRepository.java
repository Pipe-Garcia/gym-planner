package com.gymplanner.student.history;

import com.gymplanner.routine.RoutineExercise;
import com.gymplanner.routine.RoutineStatus;
import com.gymplanner.shared.blocks.BlockStructuralType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PreviousLoadsRepository extends JpaRepository<RoutineExercise, Long> {

    @Query("""
            SELECT re.id
            FROM RoutineExercise re
            JOIN re.block b
            JOIN b.day d
            JOIN d.routine r
            WHERE r.student.id = :studentId
              AND r.student.gym.id = :gymId
              AND re.exercise.id = :exerciseId
              AND r.status IN :statuses
              AND (:excludeRoutineId IS NULL OR r.id <> :excludeRoutineId)
              AND (:structuralType IS NULL OR b.structuralType = :structuralType)
            ORDER BY COALESCE(r.finishedDate, r.assignedDate) DESC, r.id DESC
            """)
    List<Long> findPreviousRoutineExerciseIds(
            @Param("gymId") Long gymId,
            @Param("studentId") Long studentId,
            @Param("exerciseId") Long exerciseId,
            @Param("excludeRoutineId") Long excludeRoutineId,
            @Param("structuralType") BlockStructuralType structuralType,
            @Param("statuses") Collection<RoutineStatus> statuses,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT re
            FROM RoutineExercise re
            LEFT JOIN FETCH re.sets
            LEFT JOIN FETCH re.block b
            LEFT JOIN FETCH b.day d
            LEFT JOIN FETCH d.routine r
            LEFT JOIN FETCH re.exercise ex
            WHERE re.id IN :ids
            """)
    List<RoutineExercise> findByIdsWithSets(@Param("ids") Collection<Long> ids);
}
