package com.gymplanner.routine;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoutineRepository extends JpaRepository<Routine, Long>, JpaSpecificationExecutor<Routine> {
    Optional<Routine> findByIdAndStudentGymId(Long id, Long gymId);
    Optional<Routine> findFirstByStudentIdAndStudentGymIdAndStatus(Long studentId, Long gymId, RoutineStatus status);

    @Query("""
            SELECT DISTINCT r FROM Routine r
            LEFT JOIN FETCH r.student st
            LEFT JOIN FETCH st.gym gym
            LEFT JOIN FETCH r.sourceTemplate tmpl
            LEFT JOIN FETCH r.days d
            LEFT JOIN FETCH d.blocks b
            LEFT JOIN FETCH b.exercises e
            LEFT JOIN FETCH e.sets s
            LEFT JOIN FETCH e.exercise ex
            LEFT JOIN FETCH ex.tags tags
            WHERE r.id = :id
            """)
    Optional<Routine> findByIdWithFullStructure(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT r FROM Routine r
            LEFT JOIN FETCH r.student st
            LEFT JOIN FETCH st.gym gym
            LEFT JOIN FETCH r.sourceTemplate tmpl
            LEFT JOIN FETCH r.days d
            LEFT JOIN FETCH d.blocks b
            LEFT JOIN FETCH b.exercises e
            LEFT JOIN FETCH e.sets s
            LEFT JOIN FETCH e.exercise ex
            LEFT JOIN FETCH ex.tags tags
            WHERE r.id = :id AND gym.id = :gymId
            """)
    Optional<Routine> findByIdWithFullStructure(@Param("id") Long id, @Param("gymId") Long gymId);

    @Query("SELECT COUNT(d) FROM RoutineDay d WHERE d.routine.id = :routineId")
    long countDays(@Param("routineId") Long routineId);

    @Query("SELECT COUNT(b) FROM RoutineBlock b WHERE b.day.routine.id = :routineId")
    long countBlocks(@Param("routineId") Long routineId);

    @Query("SELECT COUNT(e) FROM RoutineExercise e WHERE e.block.day.routine.id = :routineId")
    long countExercises(@Param("routineId") Long routineId);

    @Query("SELECT r FROM Routine r WHERE r.student.id = :studentId AND r.student.gym.id = :gymId AND r.status IN :statuses ORDER BY r.assignedDate DESC")
    java.util.List<Routine> findByStudentAndStatuses(@Param("studentId") Long studentId, @Param("gymId") Long gymId, @Param("statuses") Collection<RoutineStatus> statuses);
}
