
package com.example.concertticketingsystem.controller;

import com.example.concertticketingsystem.dto.response.LoyaltyCardResponse;
import com.example.concertticketingsystem.service.LoyaltyCardService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Fidélité")
public class LoyaltyCardController {

    private final LoyaltyCardService loyaltyCardService;

    public LoyaltyCardController(LoyaltyCardService loyaltyCardService) {
        this.loyaltyCardService = loyaltyCardService;
    }

    @PostMapping("/{id}/carte-fidelite")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @SecurityRequirement(name = "basicAuth")
    public ResponseEntity<LoyaltyCardResponse> purchaseCard(
            @PathVariable @Positive Long id) {

        LoyaltyCardResponse response =
                loyaltyCardService.purchaseCard(id);

        return ResponseEntity
                .created(URI.create("/api/clients/" + id + "/carte-fidelite"))
                .body(response);
    }
}
