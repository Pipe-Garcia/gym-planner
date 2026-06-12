package com.gymplanner.pdf.dto;

public record PdfMetadataDto(
        PdfGymDto gym,
        String routineName,
        String studentFullName,
        String assignedDateFormatted,
        String assignedDateIso,
        String objective,
        String sport) {

    public record PdfGymDto(
            String name,
            String ownerName,
            String phone,
            String email,
            String address,
            String primaryColor,
            String initials,
            String logoUrl) {
    }
}
