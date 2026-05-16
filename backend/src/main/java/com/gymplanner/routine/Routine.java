package com.gymplanner.routine;

import com.gymplanner.shared.audit.AuditableEntity;
import com.gymplanner.student.Student;
import com.gymplanner.template.TrainingTemplate;
import com.gymplanner.user.User;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "routines")
public class Routine extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String name;

    @Size(max = 150)
    @Column(length = 150)
    private String objective;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_template_id")
    private TrainingTemplate sourceTemplate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoutineStatus status = RoutineStatus.ACTIVE;

    @NotNull
    @Column(name = "assigned_date", nullable = false)
    private LocalDate assignedDate;

    @Column(name = "finished_date")
    private LocalDate finishedDate;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finished_by_user_id")
    private User finishedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_routine_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Routine previousRoutine;

    @Column(name = "general_notes", columnDefinition = "TEXT")
    private String generalNotes;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @Column(name = "closure_notes", columnDefinition = "TEXT")
    private String closureNotes;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private Set<RoutineDay> days = new LinkedHashSet<>();
}
