package com.example.concertticketingsystem.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import com.example.concertticketingsystem.dto.ConcertRequest;
import com.example.concertticketingsystem.dto.ConcertResponse;
import com.example.concertticketingsystem.service.ConcertService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConcertController {
    private final ConcertService concerts;

    public ConcertController(ConcertService concerts) {
        this.concerts = concerts;
    }

    @GetMapping("/api/concerts")
    @PreAuthorize("permitAll()")
    public List<ConcertResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @RequestParam(required = false) @Positive Long artiste) {
        return concerts.list(du, au, artiste);
    }

    @GetMapping("/api/concerts/{id}")
    @PreAuthorize("permitAll()")
    public ConcertResponse get(@PathVariable @Positive Long id) {
        return concerts.get(id);
    }

    @PostMapping("/api/production/concerts")
    @PreAuthorize("hasAnyRole('PRODUCTEUR', 'ADMIN')")
    public ResponseEntity<ConcertResponse> create(@Valid @RequestBody ConcertRequest request) {
        ConcertResponse response = concerts.create(request);
        return ResponseEntity.created(URI.create("/api/concerts/" + response.id())).body(response);
    }
}
