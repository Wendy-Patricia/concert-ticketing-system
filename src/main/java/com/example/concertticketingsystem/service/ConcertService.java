package com.example.concertticketingsystem.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.example.concertticketingsystem.dto.ConcertRequest;
import com.example.concertticketingsystem.dto.ConcertResponse;
import com.example.concertticketingsystem.entity.Artist;
import com.example.concertticketingsystem.entity.Concert;
import com.example.concertticketingsystem.entity.Venue;
import com.example.concertticketingsystem.exception.CatalogNotFoundException;
import com.example.concertticketingsystem.exception.CatalogValidationException;
import com.example.concertticketingsystem.repository.ArtistRepository;
import com.example.concertticketingsystem.repository.ConcertRepository;
import com.example.concertticketingsystem.repository.VenueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConcertService {
    private final ConcertRepository concerts;
    private final ArtistRepository artists;
    private final VenueRepository venues;

    public ConcertService(ConcertRepository concerts, ArtistRepository artists, VenueRepository venues) {
        this.concerts = concerts;
        this.artists = artists;
        this.venues = venues;
    }

    public List<ConcertResponse> list(LocalDate du, LocalDate au, Long artiste) {
        if (du != null && au != null && du.isAfter(au)) {
            throw new CatalogValidationException("La date 'du' doit précéder ou être égale à la date 'au'.");
        }
        return concerts.search(du == null ? null : du.atStartOfDay(),
                        au == null ? null : au.atTime(LocalTime.MAX.withNano(999_999_000)), artiste)
                .stream().map(ConcertResponse::from).toList();
    }

    public ConcertResponse get(Long id) {
        return ConcertResponse.from(concerts.findById(id).orElseThrow(() ->
                new CatalogNotFoundException("CONCERT_INTROUVABLE", "Concert introuvable.")));
    }

    @Transactional
    public ConcertResponse create(ConcertRequest request) {
        Artist artist = artists.findById(request.artistId()).orElseThrow(() ->
                new CatalogNotFoundException("ARTISTE_INTROUVABLE", "Artiste introuvable."));
        Venue venue = venues.findById(request.venueId()).orElseThrow(() ->
                new CatalogNotFoundException("SALLE_INTROUVABLE", "Salle introuvable."));
        return ConcertResponse.from(concerts.saveAndFlush(new Concert(
                request.title(), artist, venue, request.dateTime(), request.unitPrice())));
    }
}
