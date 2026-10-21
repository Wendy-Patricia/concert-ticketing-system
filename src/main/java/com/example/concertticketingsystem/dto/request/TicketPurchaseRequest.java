
package com.example.concertticketingsystem.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TicketPurchaseRequest(
        @NotNull Long concertId,
        @NotNull Long customerId,
        @NotNull @Min(1) @Max(6) Integer quantity
) {}
