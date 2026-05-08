package com.gymplanner.gym.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateGymRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 150) String ownerName,
        @Size(max = 50) String phone,
        @Email @Size(max = 150) String email,
        @Size(max = 255) String address,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "must be a valid hex color")
        @Size(max = 7) String primaryColor,
        @Size(max = 500) String logoUrl) {
}
