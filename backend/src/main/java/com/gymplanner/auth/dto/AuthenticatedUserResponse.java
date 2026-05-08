package com.gymplanner.auth.dto;

import com.gymplanner.user.UserRole;

public record AuthenticatedUserResponse(
        Long id,
        String email,
        String fullName,
        UserRole role,
        Long gymId) {
}
