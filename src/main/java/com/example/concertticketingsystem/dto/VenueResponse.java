package com.example.concertticketingsystem.dto;

import com.example.concertticketingsystem.entity.Venue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Salle enregistrée", requiredProperties = {"id", "name", "city", "capacity"})
public record VenueResponse(Long id, String name, String city, Integer capacity) {
    public static VenueResponse from(Venue venue) {
        return new VenueResponse(venue.getId(), venue.getName(), venue.getCity(), venue.getCapacity());
    }
}
