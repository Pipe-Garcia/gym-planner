package com.gymplanner.routine;

import com.gymplanner.shared.blocks.SetKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "routine_exercise_sets", uniqueConstraints = @UniqueConstraint(name = "uk_res_re_setnum", columnNames = {"routine_exercise_id", "set_number"}))
public class RoutineExerciseSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "routine_exercise_id", nullable = false)
    private RoutineExercise routineExercise;

    @Column(name = "set_number", nullable = false)
    private int setNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "set_kind", nullable = false, length = 30)
    private SetKind setKind = SetKind.NORMAL;

    @Column(name = "target_reps")
    private Integer targetReps;

    @Column(name = "target_reps_min")
    private Integer targetRepsMin;

    @Column(name = "target_reps_max")
    private Integer targetRepsMax;

    @Column(name = "target_weight_kg", precision = 6, scale = 2)
    private BigDecimal targetWeightKg;

    @Column(name = "target_time_seconds")
    private Integer targetTimeSeconds;

    @Column(name = "target_distance_meters", precision = 7, scale = 2)
    private BigDecimal targetDistanceMeters;

    @Column(name = "rest_after_seconds")
    private Integer restAfterSeconds;

    @Column(length = 20)
    private String tempo;

    private Integer rpe;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "to_failure", nullable = false)
    private boolean toFailure;
}
