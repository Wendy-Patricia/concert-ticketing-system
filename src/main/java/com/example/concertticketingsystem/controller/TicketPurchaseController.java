
package com.example.concertticketingsystem.controller;

import com.example.concertticketingsystem.dto.request.TicketPurchaseRequest;
import com.example.concertticketingsystem.dto.response.TicketPurchaseResponse;
import com.example.concertticketingsystem.service.TicketPurchaseService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;

@RestController
@RequestMapping("/api/achats")
@Tag(name = "Achats")
public class TicketPurchaseController {

    private final TicketPurchaseService ticketPurchaseService;

    public TicketPurchaseController(
            TicketPurchaseService ticketPurchaseService) {
        this.ticketPurchaseService = ticketPurchaseService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @SecurityRequirement(name = "basicAuth")
    public ResponseEntity<TicketPurchaseResponse> purchase(
            @Valid @RequestBody TicketPurchaseRequest request) {

        TicketPurchaseResponse response =
                ticketPurchaseService.purchase(request);

        return ResponseEntity
                .created(URI.create("/api/achats/" + response.code()))
                .body(response);
    }
}
