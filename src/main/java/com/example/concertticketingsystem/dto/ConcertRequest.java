package com.example.concertticketingsystem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Création d'un concert", requiredProperties = {"title", "artistId", "venueId", "dateTime", "unitPrice"})
public record ConcertRequest(
        @Schema(example = "Jazz Night") @NotBlank @Size(max = 255) String title,
        @Schema(description = "Identifiant d'un artiste existant", example = "1") @NotNull @Positive Long artistId,
        @Schema(description = "Identifiant d'une salle existante", example = "1") @NotNull @Positive Long venueId,
        @Schema(description = "Date et heure locales ISO, obligatoirement futures au moment de l'appel", example = "2027-06-15T20:30:00")
        @NotNull @Future LocalDateTime dateTime,
        @Schema(description = "Prix unitaire positif, au plus 8 chiffres entiers et 2 décimales", example = "42.50",
                minimum = "0", exclusiveMinimum = true, maximum = "99999999.99", multipleOf = 0.01)
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal unitPrice) {
}
