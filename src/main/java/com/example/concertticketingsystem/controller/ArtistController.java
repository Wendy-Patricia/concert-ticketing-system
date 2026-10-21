package com.example.concertticketingsystem.controller;

import com.example.concertticketingsystem.dto.ArtistRequest;
import com.example.concertticketingsystem.dto.ArtistResponse;
import com.example.concertticketingsystem.dto.ApiError;
import com.example.concertticketingsystem.service.ArtistService;
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
@RequestMapping(value = "/api/production/artistes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Artistes", description = "Création des artistes du catalogue")
public class ArtistController {
    private final ArtistService artists;

    public ArtistController(ArtistService artists) {
        this.artists = artists;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PRODUCTEUR', 'ADMIN')")
    @Operation(summary = "Créer un artiste", description = "PRODUCTEUR ou ADMIN. Le nom est obligatoire et unique.")
    @SecurityRequirement(name = "basicAuth")
    @ApiResponse(responseCode = "201", description = "Artiste créé", content = @Content(schema = @Schema(implementation = ArtistResponse.class)))
    @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Authentification absente ou incorrecte", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Nom déjà utilisé ou conflit d'intégrité", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ArtistResponse create(@Valid @RequestBody ArtistRequest request) {
        return artists.create(request);
    }
}
