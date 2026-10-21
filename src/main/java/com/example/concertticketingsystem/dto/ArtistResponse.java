package com.example.concertticketingsystem.dto;

import com.example.concertticketingsystem.entity.Artist;

public record ArtistResponse(Long id, String name, String musicalGenre) {
    public static ArtistResponse from(Artist artist) {
        return new ArtistResponse(artist.getId(), artist.getName(), artist.getMusicalGenre());
    }
}
