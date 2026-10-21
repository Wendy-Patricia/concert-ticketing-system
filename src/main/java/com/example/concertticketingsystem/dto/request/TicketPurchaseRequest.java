
package com.example.concertticketingsystem.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TicketPurchaseRequest(
        @NotNull @Positive Long concertId,
        @NotNull @Positive Long customerId,
        @NotNull @Min(1) @Max(6) Integer quantity
) {}
