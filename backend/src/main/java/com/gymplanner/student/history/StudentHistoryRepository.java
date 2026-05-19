package com.gymplanner.student.history;

import com.gymplanner.routine.Routine;
import com.gymplanner.routine.RoutineExercise;
import com.gymplanner.routine.RoutineStatus;
import com.gymplanner.shared.blocks.BlockStructuralType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentHistoryRepository extends JpaRepository<Routine, Long> {

    @Query("""
            SELECT COUNT(r)
            FROM Routine r
            WHERE r.student.id = :studentId
              AND r.student.gym.id = :gymId
              AND r.status IN :statuses
            """)
    long countIncludedRoutines(@Param("gymId") Long gymId, @Param("studentId") Long studentId,
            @Param("statuses") Collection<RoutineStatus> statuses);

    @Query("""
            SELECT r
            FROM Routine r
            WHERE r.student.id = :studentId
              AND r.student.gym.id = :gymId
              AND r.status = com.gymplanner.routine.RoutineStatus.ACTIVE
            ORDER BY r.assignedDate DESC, r.id DESC
            """)
    List<Routine> findActiveRoutines(@Param("gymId") Long gymId, @Param("studentId") Long studentId, Pageable pageable);

    @Query("""
            SELECT COUNT(DISTINCT re.exercise.id)
            FROM RoutineExercise re
            JOIN re.block b
            JOIN b.day d
            JOIN d.routine r
            WHERE r.student.id = :studentId
              AND r.student.gym.id = :gymId
              AND r.status IN :statuses
            """)
    long countDistinctExercises(@Param("gymId") Long gymId, @Param("studentId") Long studentId,
            @Param("statuses") Collection<RoutineStatus> statuses);

    @Query("""
            SELECT MIN(r.assignedDate)
            FROM Routine r
            WHERE r.student.id = :studentId
              AND r.student.gym.id = :gymId
              AND r.status IN :statuses
            """)
    LocalDate findTrainingSince(@Param("gymId") Long gymId, @Param("studentId") Long studentId,
            @Param("statuses") Collection<RoutineStatus> statuses);

    @Query(value = """
            SELECT r
            FROM Routine r
            LEFT JOIN FETCH r.sourceTemplate
            WHERE r.student.id = :studentId
              AND r.student.gym.id = :gymId
              AND r.status IN :statuses
            """,
            countQuery = """
            SELECT COUNT(r)
            FROM Routine r
            WHERE r.student.id = :studentId
              AND r.student.gym.id = :gymId
              AND r.status IN :statuses
            """)
    Page<Routine> findTimelineRoutines(@Param("gymId") Long gymId, @Param("studentId") Long studentId,
            @Param("statuses") Collection<RoutineStatus> statuses, Pageable pageable);

    @Query("""
            SELECT d.routine.id AS id, COUNT(d) AS count
            FROM RoutineDay d
            WHERE d.routine.id IN :routineIds
            GROUP BY d.routine.id
            """)
    List<IdCountProjection> countDaysByRoutineIds(@Param("routineIds") Collection<Long> routineIds);

    @Query("""
            SELECT b.day.routine.id AS id, COUNT(b) AS count
            FROM RoutineBlock b
            WHERE b.day.routine.id IN :routineIds
            GROUP BY b.day.routine.id
            """)
    List<IdCountProjection> countBlocksByRoutineIds(@Param("routineIds") Collection<Long> routineIds);

    @Query("""
            SELECT re.block.day.routine.id AS id, COUNT(re) AS count
            FROM RoutineExercise re
            WHERE re.block.day.routine.id IN :routineIds
            GROUP BY re.block.day.routine.id
            """)
    List<IdCountProjection> countExercisesByRoutineIds(@Param("routineIds") Collection<Long> routineIds);

    @Query(value = """
            SELECT re.exercise.id
            FROM RoutineExercise re
            JOIN re.exercise ex
            JOIN re.block b
            JOIN b.day d
            JOIN d.routine r
            WHERE r.student.id = :studentId
              AND r.student.gym.id = :gymId
              AND r.status IN :statuses
            GROUP BY re.exercise.id
            ORDER BY MAX(COALESCE(r.finishedDate, r.assignedDate)) DESC, re.exercise.id DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT re.exercise.id)
            FROM RoutineExercise re
            JOIN re.exercise ex
            JOIN re.block b
            JOIN b.day d
            JOIN d.routine r
            WHERE r.student.id = :studentId
              AND r.student.gym.id = :gymId
              AND r.status IN :statuses
            """)
    Page<Long> findExerciseHistoryExerciseIds(@Param("gymId") Long gymId, @Param("studentId") Long studentId,
            @Param("statuses") Collection<RoutineStatus> statuses, Pageable pageable);

    @Query(value = """
            SELECT re.exercise.id
            FROM RoutineExercise re
            JOIN re.exercise ex
            JOIN re.block b
            JOIN b.day d
            JOIN d.routine r
            WHERE r.student.id = :studentId
              AND r.student.gym.id = :gymId
              AND r.status IN :statuses
              AND LOWER(ex.name) LIKE CONCAT('%', :search, '%')
            GROUP BY re.exercise.id
            ORDER BY MAX(COALESCE(r.finishedDate, r.assignedDate)) DESC, re.exercise.id DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT re.exercise.id)
            FROM RoutineExercise re
            JOIN re.exercise ex
            JOIN re.block b
            JOIN b.day d
            JOIN d.routine r
            WHERE r.student.id = :studentId
              AND r.student.gym.id = :gymId
              AND r.status IN :statuses
              AND LOWER(ex.name) LIKE CONCAT('%', :search, '%')
            """)
    Page<Long> findExerciseHistoryExerciseIdsBySearch(@Param("gymId") Long gymId, @Param("studentId") Long studentId,
            @Param("statuses") Collection<RoutineStatus> statuses, @Param("search") String search, Pageable pageable);

    @Query("""
            SELECT ex.id AS exerciseId,
                   ex.name AS exerciseName,
                   r.id AS routineId,
                   r.name AS routineName,
                   COALESCE(r.finishedDate, r.assignedDate) AS effectiveDate,
                   b.structuralType AS structuralType
            FROM RoutineExercise re
            JOIN re.exercise ex
            JOIN re.block b
            JOIN b.day d
            JOIN d.routine r
            WHERE r.student.id = :studentId
              AND r.student.gym.id = :gymId
              AND r.status IN :statuses
              AND ex.id IN :exerciseIds
            """)
    List<ExerciseAppearanceProjection> findExerciseAppearances(@Param("gymId") Long gymId, @Param("studentId") Long studentId,
            @Param("statuses") Collection<RoutineStatus> statuses, @Param("exerciseIds") Collection<Long> exerciseIds);

    @Query(value = """
            SELECT re.id
            FROM RoutineExercise re
            JOIN re.block b
            JOIN b.day d
            JOIN d.routine r
            WHERE r.student.id = :studentId
              AND r.student.gym.id = :gymId
              AND re.exercise.id = :exerciseId
              AND r.status IN :statuses
            ORDER BY COALESCE(r.finishedDate, r.assignedDate) DESC,
                     r.id DESC,
                     d.orderIndex ASC,
                     b.orderIndex ASC,
                     re.orderIndex ASC
            """,
            countQuery = """
            SELECT COUNT(re)
            FROM RoutineExercise re
            JOIN re.block b
            JOIN b.day d
            JOIN d.routine r
            WHERE r.student.id = :studentId
              AND r.student.gym.id = :gymId
              AND re.exercise.id = :exerciseId
              AND r.status IN :statuses
            """)
    Page<Long> findOccurrenceIds(@Param("gymId") Long gymId, @Param("studentId") Long studentId,
            @Param("exerciseId") Long exerciseId, @Param("statuses") Collection<RoutineStatus> statuses, Pageable pageable);

    @Query("""
            SELECT DISTINCT re
            FROM RoutineExercise re
            LEFT JOIN FETCH re.sets
            LEFT JOIN FETCH re.exercise
            LEFT JOIN FETCH re.block b
            LEFT JOIN FETCH b.day d
            LEFT JOIN FETCH d.routine r
            WHERE re.id IN :ids
            """)
    List<RoutineExercise> findOccurrencesWithContextAndSets(@Param("ids") Collection<Long> ids);

    interface IdCountProjection {
        Long getId();
        long getCount();
    }

    interface ExerciseAppearanceProjection {
        Long getExerciseId();
        String getExerciseName();
        Long getRoutineId();
        String getRoutineName();
        LocalDate getEffectiveDate();
        BlockStructuralType getStructuralType();
    }
}
