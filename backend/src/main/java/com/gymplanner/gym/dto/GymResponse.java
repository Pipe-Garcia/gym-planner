package com.gymplanner.gym.dto;

import java.time.Instant;

public record GymResponse(
        Long id,
        String name,
        String ownerName,
        String phone,
        String email,
        String address,
        String logoUrl,
        String primaryColor,
        Instant createdAt,
        Instant updatedAt) {
}
