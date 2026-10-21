
package com.example.concertticketingsystem.service;

import com.example.concertticketingsystem.entity.Customer;
import com.example.concertticketingsystem.entity.LoyaltyCard;
import com.example.concertticketingsystem.exception.BusinessConflictException;
import com.example.concertticketingsystem.repository.CustomerRepository;
import com.example.concertticketingsystem.repository.LoyaltyCardRepository;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoyaltyCardServiceTest {

    private final LoyaltyCardRepository cardRepository =
            mock(LoyaltyCardRepository.class);

    private final CustomerRepository customerRepository =
            mock(CustomerRepository.class);

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-10-21T10:00:00Z"),
            ZoneOffset.UTC
    );

    private final LoyaltyCardService service =
            new LoyaltyCardService(
                    cardRepository,
                    customerRepository,
                    clock
            );

    @Test
    void shouldCreateCardValidForOneYear() {

        Customer customer = mock(Customer.class);

        when(customerRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(customer));

        when(cardRepository.save(any(LoyaltyCard.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.purchaseCard(1L);

        assertEquals(
                LocalDate.of(2026, 10, 21),
                response.purchasedAt()
        );

        assertEquals(
                LocalDate.of(2027, 10, 21),
                response.expiresAt()
        );

        verify(cardRepository).save(any(LoyaltyCard.class));
    }

    @Test
    void shouldRejectExistingActiveCard() {

        Customer customer = mock(Customer.class);

        when(customerRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(customer));

        when(cardRepository
                .existsByCustomerIdAndPurchasedAtLessThanEqualAndExpiresAtGreaterThan(
                        eq(1L), any(LocalDate.class), any(LocalDate.class)
                ))
                .thenReturn(true);

        assertThrows(
                BusinessConflictException.class,
                () -> service.purchaseCard(1L)
        );

        verify(cardRepository, never())
                .save(any(LoyaltyCard.class));
    }

    @Test
    void shouldDetectExpiredCard() {

        LoyaltyCard card = new LoyaltyCard(
                mock(Customer.class),
                LocalDate.of(2025, 10, 21),
                LocalDate.of(2026, 10, 21)
        );

        assertFalse(card.isActive(LocalDate.of(2026, 10, 21)));
        assertTrue(card.isActive(LocalDate.of(2026, 10, 20)));
    }
}
