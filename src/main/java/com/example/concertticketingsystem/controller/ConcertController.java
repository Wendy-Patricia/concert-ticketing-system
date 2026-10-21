package com.example.concertticketingsystem.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import com.example.concertticketingsystem.dto.ConcertRequest;
import com.example.concertticketingsystem.dto.ConcertResponse;
import com.example.concertticketingsystem.dto.ApiError;
import com.example.concertticketingsystem.service.ConcertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Concerts", description = "Consultation publique et création des concerts")
public class ConcertController {
    private final ConcertService concerts;

    public ConcertController(ConcertService concerts) {
        this.concerts = concerts;
    }

    @GetMapping("/api/concerts")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Lister les concerts", description = "Accès public. Filtres combinés par ET ; tri par date puis identifiant. "
            + "Une période inversée renvoie 400 ; aucun résultat renvoie une liste vide avec 200.")
    @ApiResponse(responseCode = "200", description = "Liste des concerts", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConcertResponse.class))))
    @ApiResponse(responseCode = "400", description = "Filtres invalides", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public List<ConcertResponse> list(
            @Parameter(description = "Date de début inclusive (YYYY-MM-DD)", example = "2027-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @Parameter(description = "Date de fin inclusive, jusqu'à 23:59:59.999999 (YYYY-MM-DD)", example = "2027-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @Parameter(description = "Identifiant positif de l'artiste ; un identifiant inconnu donne une liste vide", example = "1")
            @RequestParam(required = false) @Positive Long artiste) {
        return concerts.list(du, au, artiste);
    }

    @GetMapping("/api/concerts/{id}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Consulter un concert", description = "Accès public. Réponse avec les DTO de l'artiste et de la salle.")
    @ApiResponse(responseCode = "200", description = "Détail du concert", content = @Content(schema = @Schema(implementation = ConcertResponse.class)))
    @ApiResponse(responseCode = "400", description = "Identifiant invalide", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Concert introuvable", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ConcertResponse get(@Parameter(description = "Identifiant positif du concert", example = "1") @PathVariable @Positive Long id) {
        return concerts.get(id);
    }

    @PostMapping("/api/production/concerts")
    @PreAuthorize("hasAnyRole('PRODUCTEUR', 'ADMIN')")
    @Operation(summary = "Créer un concert", description = "PRODUCTEUR ou ADMIN. Artiste et salle existants, date future et prix strictement positif.")
    @SecurityRequirement(name = "basicAuth")
    @ApiResponse(responseCode = "201", description = "Concert créé", headers = @Header(name = "Location", description = "URL du concert créé", schema = @Schema(type = "string")), content = @Content(schema = @Schema(implementation = ConcertResponse.class)))
    @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Authentification absente ou incorrecte", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Artiste ou salle introuvable", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Conflit d'intégrité, notamment référence supprimée pendant la création", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<ConcertResponse> create(@Valid @RequestBody ConcertRequest request) {
        ConcertResponse response = concerts.create(request);
        return ResponseEntity.created(URI.create("/api/concerts/" + response.id())).body(response);
    }
}
