package com.example.concertticketingsystem.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VenueRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String city,
        @NotNull @Min(50) @Max(50_000) Integer capacity) {
}
