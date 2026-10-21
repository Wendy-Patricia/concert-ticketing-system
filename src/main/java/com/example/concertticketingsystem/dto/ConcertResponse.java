package com.example.concertticketingsystem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.concertticketingsystem.entity.Concert;

public record ConcertResponse(Long id, String title, ArtistResponse artist, VenueResponse venue,
                              LocalDateTime dateTime, BigDecimal unitPrice) {
    public static ConcertResponse from(Concert concert) {
        return new ConcertResponse(concert.getId(), concert.getTitle(),
                ArtistResponse.from(concert.getArtist()), VenueResponse.from(concert.getVenue()),
                concert.getDateTime(), concert.getUnitPrice());
    }
}
