package com.example.concertticketingsystem.controller;

import com.example.concertticketingsystem.dto.VenueRequest;
import com.example.concertticketingsystem.dto.VenueResponse;
import com.example.concertticketingsystem.dto.ApiError;
import com.example.concertticketingsystem.service.VenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/admin/salles", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Salles", description = "Création des salles du catalogue")
public class VenueController {
    private final VenueService venues;

    public VenueController(VenueService venues) {
        this.venues = venues;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer une salle", description = "ADMIN. Capacité entre 50 et 50 000 ; couple (name, city) unique.")
    @SecurityRequirement(name = "basicAuth")
    @ApiResponse(responseCode = "201", description = "Salle créée", content = @Content(schema = @Schema(implementation = VenueResponse.class)))
    @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Authentification absente ou incorrecte", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Salle déjà existante dans cette ville ou conflit d'intégrité", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public VenueResponse create(@Valid @RequestBody VenueRequest request) {
        return venues.create(request);
    }
}
