package com.gymplanner.gym;

import com.gymplanner.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "gyms")
public class Gym extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String name;

    @Size(max = 150)
    @Column(name = "owner_name", length = 150)
    private String ownerName;

    @Size(max = 50)
    @Column(length = 50)
    private String phone;

    @Email
    @Size(max = 150)
    @Column(length = 150)
    private String email;

    @Size(max = 255)
    @Column(length = 255)
    private String address;

    @Size(max = 500)
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "must be a valid hex color")
    @Size(max = 7)
    @Column(name = "primary_color", length = 7)
    private String primaryColor;
}
