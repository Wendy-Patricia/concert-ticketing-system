package com.example.concertticketingsystem.dto;

import com.example.concertticketingsystem.entity.Venue;

public record VenueResponse(Long id, String name, String city, Integer capacity) {
    public static VenueResponse from(Venue venue) {
        return new VenueResponse(venue.getId(), venue.getName(), venue.getCity(), venue.getCapacity());
    }
}
