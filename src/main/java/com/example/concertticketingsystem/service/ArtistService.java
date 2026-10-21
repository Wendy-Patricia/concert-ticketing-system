package com.example.concertticketingsystem.service;

import com.example.concertticketingsystem.dto.ArtistRequest;
import com.example.concertticketingsystem.dto.ArtistResponse;
import com.example.concertticketingsystem.entity.Artist;
import com.example.concertticketingsystem.exception.CatalogConflictException;
import com.example.concertticketingsystem.repository.ArtistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArtistService {
    private final ArtistRepository artists;

    public ArtistService(ArtistRepository artists) {
        this.artists = artists;
    }

    @Transactional
    public ArtistResponse create(ArtistRequest request) {
        if (artists.existsByName(request.name())) {
            throw new CatalogConflictException("Un artiste portant ce nom existe déjà.");
        }
        return ArtistResponse.from(artists.saveAndFlush(new Artist(request.name(), request.musicalGenre())));
    }
}
