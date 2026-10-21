
package com.example.concertticketingsystem.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TicketPricingService {

    private static final BigDecimal LOYALTY_DISCOUNT =
            new BigDecimal("0.10");

    public BigDecimal calculateTotal(
            BigDecimal unitPrice,
            int quantity,
            boolean activeLoyaltyCard) {

        if (unitPrice == null || unitPrice.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Le prix doit être positif"
            );
        }

        if (quantity < 1 || quantity > 6) {
            throw new IllegalArgumentException(
                    "La quantité doit être comprise entre 1 et 6"
            );
        }

        BigDecimal subtotal = unitPrice.multiply(
                BigDecimal.valueOf(quantity)
        );

        if (activeLoyaltyCard) {
            subtotal = subtotal.multiply(
                    BigDecimal.ONE.subtract(LOYALTY_DISCOUNT)
            );
        }

        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal discountRate(boolean activeLoyaltyCard) {
        return activeLoyaltyCard
                ? LOYALTY_DISCOUNT
                : BigDecimal.ZERO;
    }
}
