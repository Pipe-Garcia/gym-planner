package com.gymplanner.auth.dto;

public record LoginResponse(
        String token,
        AuthenticatedUserResponse user) {
}
