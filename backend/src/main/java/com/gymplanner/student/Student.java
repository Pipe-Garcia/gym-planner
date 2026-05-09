package com.gymplanner.student;

import com.gymplanner.gym.Gym;
import com.gymplanner.shared.audit.AuditableEntity;
import com.gymplanner.student.injury.StudentInjury;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "students",
        uniqueConstraints = @UniqueConstraint(name = "uk_students_gym_doc", columnNames = {"gym_id", "document_id"}))
public class Student extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @NotBlank
    @Size(max = 100)
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Size(max = 50)
    @Column(name = "document_id", length = 50)
    private String documentId;

    @Size(max = 50)
    @Column(length = 50)
    private String phone;

    @Email
    @Size(max = 150)
    @Column(length = 150)
    private String email;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Size(max = 100)
    @Column(length = 100)
    private String sport;

    @Size(max = 150)
    @Column(length = 150)
    private String objective;

    @Size(max = 50)
    @Column(length = 50)
    private String level;

    @Column(name = "general_notes", columnDefinition = "TEXT")
    private String generalNotes;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "started_at")
    private LocalDate startedAt;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudentInjury> injuries = new ArrayList<>();
}
