package com.example.concertticketingsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ArtistRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String musicalGenre) {
}
