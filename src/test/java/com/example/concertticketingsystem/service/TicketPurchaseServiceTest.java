package com.example.concertticketingsystem.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.example.concertticketingsystem.dto.request.TicketPurchaseRequest;
import com.example.concertticketingsystem.entity.Address;
import com.example.concertticketingsystem.entity.Customer;
import com.example.concertticketingsystem.entity.TicketPurchase;
import com.example.concertticketingsystem.exception.BusinessConflictException;
import com.example.concertticketingsystem.exception.ResourceNotFoundException;
import com.example.concertticketingsystem.repository.CustomerRepository;
import com.example.concertticketingsystem.repository.LoyaltyCardRepository;
import com.example.concertticketingsystem.repository.TicketPurchaseRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TicketPurchaseServiceTest {
    private final TicketPurchaseRepository purchases = mock(TicketPurchaseRepository.class);
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final LoyaltyCardRepository cards = mock(LoyaltyCardRepository.class);
    private final ConcertAvailabilityGateway gateway = mock(ConcertAvailabilityGateway.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);
    private final TicketPurchaseService service = new TicketPurchaseService(purchases, customers, cards, gateway,
            new TicketPricingService(), clock);

    private void available(long sold) {
        when(customers.findById(2L)).thenReturn(Optional.of(new Customer("Alice", "alice@example.com", new Address("Rue", "Paris"))));
        when(gateway.findAndLockConcert(1L)).thenReturn(new ConcertAvailabilityGateway.ConcertInfo(1L, new BigDecimal("50.00"), 50));
        when(purchases.sumQuantityByConcertId(1L)).thenReturn(sold);
    }

    @Test
    void locksBeforeCountingAndPersistsComputedPrice() {
        available(45);
        var response = service.purchase(new TicketPurchaseRequest(1L, 2L, 4));
        assertThat(response.remainingSeats()).isEqualTo(1);
        assertThat(response.totalAmount()).isEqualByComparingTo("200.00");
        InOrder order = inOrder(gateway, purchases);
        order.verify(gateway).findAndLockConcert(1L);
        order.verify(purchases).sumQuantityByConcertId(1L);
        ArgumentCaptor<TicketPurchase> saved = ArgumentCaptor.forClass(TicketPurchase.class);
        order.verify(purchases).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getQuantity()).isEqualTo(4);
        assertThat(saved.getValue().getTotalAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    void appliesExistingLoyaltyPricingRule() {
        available(0);
        when(cards.existsByCustomerIdAndPurchasedAtLessThanEqualAndExpiresAtGreaterThan(eq(2L), any(), any()))
                .thenReturn(true);
        var response = service.purchase(new TicketPurchaseRequest(1L, 2L, 2));
        assertThat(response.totalAmount()).isEqualByComparingTo("90.00");
        assertThat(response.discountRate()).isEqualByComparingTo("0.10");
    }

    @Test
    void neverWritesWhenCapacityIsInsufficient() {
        available(49);
        assertThatThrownBy(() -> service.purchase(new TicketPurchaseRequest(1L, 2L, 4)))
                .isInstanceOfSatisfying(BusinessConflictException.class,
                        error -> assertThat(error.getCode()).isEqualTo("JAUGE_INSUFFISANTE"));
        verify(purchases, never()).saveAndFlush(any());
    }

    @Test
    void neverWritesWhenCustomerIsMissing() {
        assertThatThrownBy(() -> service.purchase(new TicketPurchaseRequest(1L, 2L, 1)))
                .isInstanceOfSatisfying(ResourceNotFoundException.class,
                        error -> assertThat(error.getCode()).isEqualTo("CLIENT_INTROUVABLE"));
        verifyNoInteractions(gateway, purchases);
    }

    @Test
    void rejectsQuantityOutsidePurchaseContract() {
        assertThatThrownBy(() -> service.purchase(new TicketPurchaseRequest(1L, 2L, 7)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(customers, gateway, purchases);
    }
}
