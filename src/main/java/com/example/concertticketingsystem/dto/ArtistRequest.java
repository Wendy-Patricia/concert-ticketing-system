package com.example.concertticketingsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Création d'un artiste ; le nom doit être unique", requiredProperties = {"name"})
public record ArtistRequest(
        @Schema(example = "Nina Simone") @NotBlank @Size(max = 255) String name,
        @Schema(description = "Genre musical facultatif", example = "Jazz", nullable = true)
        @Size(max = 255) String musicalGenre) {
}
