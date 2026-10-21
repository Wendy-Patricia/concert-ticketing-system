
package com.example.concertticketingsystem.dto.response;

import java.math.BigDecimal;

public record TicketPurchaseResponse(
        String code,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal discountRate,
        BigDecimal totalAmount,
        int remainingSeats
) {}
