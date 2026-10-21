package com.example.concertticketingsystem.controller;

import com.example.concertticketingsystem.dto.VenueRequest;
import com.example.concertticketingsystem.dto.VenueResponse;
import com.example.concertticketingsystem.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/salles")
public class VenueController {
    private final VenueService venues;

    public VenueController(VenueService venues) {
        this.venues = venues;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public VenueResponse create(@Valid @RequestBody VenueRequest request) {
        return venues.create(request);
    }
}
