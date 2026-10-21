
package com.example.concertticketingsystem.controller;

import com.example.concertticketingsystem.dto.response.LoyaltyCardResponse;
import com.example.concertticketingsystem.service.LoyaltyCardService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/clients")
public class LoyaltyCardController {

    private final LoyaltyCardService loyaltyCardService;

    public LoyaltyCardController(LoyaltyCardService loyaltyCardService) {
        this.loyaltyCardService = loyaltyCardService;
    }

    @PostMapping("/{id}/carte-fidelite")
    public ResponseEntity<LoyaltyCardResponse> purchaseCard(
            @PathVariable Long id) {

        LoyaltyCardResponse response =
                loyaltyCardService.purchaseCard(id);

        return ResponseEntity
                .created(URI.create("/api/clients/" + id + "/carte-fidelite"))
                .body(response);
    }
}
