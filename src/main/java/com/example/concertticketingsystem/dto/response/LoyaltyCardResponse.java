
package com.example.concertticketingsystem.dto.response;

import java.time.LocalDate;

public record LoyaltyCardResponse(
        Long id,
        Long customerId,
        LocalDate purchasedAt,
        LocalDate expiresAt
) {}
