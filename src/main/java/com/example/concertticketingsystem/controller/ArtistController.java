package com.example.concertticketingsystem.controller;

import com.example.concertticketingsystem.dto.ArtistRequest;
import com.example.concertticketingsystem.dto.ArtistResponse;
import com.example.concertticketingsystem.service.ArtistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/production/artistes")
public class ArtistController {
    private final ArtistService artists;

    public ArtistController(ArtistService artists) {
        this.artists = artists;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PRODUCTEUR', 'ADMIN')")
    public ArtistResponse create(@Valid @RequestBody ArtistRequest request) {
        return artists.create(request);
    }
}
