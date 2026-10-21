
package com.example.concertticketingsystem.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TicketPricingServiceTest {

    private final TicketPricingService service =
            new TicketPricingService();

    @Test
    void shouldCalculatePriceWithoutDiscount() {
        BigDecimal total = service.calculateTotal(
                new BigDecimal("50.00"),
                2,
                false
        );

        assertEquals(new BigDecimal("100.00"), total);
    }

    @Test
    void shouldApplyTenPercentDiscount() {
        BigDecimal total = service.calculateTotal(
                new BigDecimal("50.00"),
                2,
                true
        );

        assertEquals(new BigDecimal("90.00"), total);
    }

    @Test
    void shouldRoundToTwoDecimals() {
        BigDecimal total = service.calculateTotal(
                new BigDecimal("19.99"),
                3,
                true
        );

        assertEquals(new BigDecimal("53.97"), total);
    }

    @Test
    void shouldRejectInvalidQuantity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.calculateTotal(
                        new BigDecimal("50.00"),
                        7,
                        false
                )
        );
    }
}
