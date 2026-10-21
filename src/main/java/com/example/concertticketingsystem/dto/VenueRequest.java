package com.example.concertticketingsystem.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Création d'une salle ; le couple (name, city) est unique", requiredProperties = {"name", "city", "capacity"})
public record VenueRequest(
        @Schema(example = "Olympia") @NotBlank @Size(max = 255) String name,
        @Schema(example = "Paris") @NotBlank @Size(max = 255) String city,
        @Schema(description = "Capacité inclusive de 50 à 50 000", example = "500", minimum = "50", maximum = "50000")
        @NotNull @Min(50) @Max(50_000) Integer capacity) {
}
