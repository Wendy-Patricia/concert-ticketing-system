package com.example.concertticketingsystem.dto;

import com.example.concertticketingsystem.entity.Artist;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Artiste enregistré", requiredProperties = {"id", "name"})
public record ArtistResponse(Long id, String name, String musicalGenre) {
    public static ArtistResponse from(Artist artist) {
        return new ArtistResponse(artist.getId(), artist.getName(), artist.getMusicalGenre());
    }
}
