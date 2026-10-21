package com.example.concertticketingsystem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ConcertRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull @Positive Long artistId,
        @NotNull @Positive Long venueId,
        @NotNull @Future LocalDateTime dateTime,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal unitPrice) {
}
