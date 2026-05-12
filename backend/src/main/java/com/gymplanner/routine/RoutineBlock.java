package com.gymplanner.routine;

import com.gymplanner.shared.blocks.BlockPurpose;
import com.gymplanner.shared.blocks.BlockStructuralType;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "routine_blocks", uniqueConstraints = @UniqueConstraint(name = "uk_routine_blocks_day_order", columnNames = {"routine_day_id", "order_index"}))
public class RoutineBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "routine_day_id", nullable = false)
    private RoutineDay day;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String title;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "structural_type", nullable = false, length = 30)
    private BlockStructuralType structuralType;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private BlockPurpose purpose;

    @Column(name = "total_duration_seconds")
    private Integer totalDurationSeconds;

    @Column(name = "target_rounds")
    private Integer targetRounds;

    @Column(name = "block_notes", columnDefinition = "TEXT")
    private String blockNotes;

    @OneToMany(mappedBy = "block", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private Set<RoutineExercise> exercises = new LinkedHashSet<>();
}
