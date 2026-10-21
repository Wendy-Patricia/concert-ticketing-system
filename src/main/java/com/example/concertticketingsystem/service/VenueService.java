package com.example.concertticketingsystem.service;

import com.example.concertticketingsystem.dto.VenueRequest;
import com.example.concertticketingsystem.dto.VenueResponse;
import com.example.concertticketingsystem.entity.Venue;
import com.example.concertticketingsystem.exception.CatalogConflictException;
import com.example.concertticketingsystem.repository.VenueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VenueService {
    private final VenueRepository venues;

    public VenueService(VenueRepository venues) {
        this.venues = venues;
    }

    @Transactional
    public VenueResponse create(VenueRequest request) {
        if (venues.existsByNameAndCity(request.name(), request.city())) {
            throw new CatalogConflictException("Une salle portant ce nom existe déjà dans cette ville.");
        }
        return VenueResponse.from(venues.saveAndFlush(
                new Venue(request.name(), request.city(), request.capacity())));
    }
}
