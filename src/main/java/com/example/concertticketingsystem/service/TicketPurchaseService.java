
package com.example.concertticketingsystem.service;

import com.example.concertticketingsystem.dto.request.TicketPurchaseRequest;
import com.example.concertticketingsystem.dto.response.TicketPurchaseResponse;
import com.example.concertticketingsystem.entity.Customer;
import com.example.concertticketingsystem.entity.TicketPurchase;
import com.example.concertticketingsystem.exception.BusinessConflictException;
import com.example.concertticketingsystem.exception.ResourceNotFoundException;
import com.example.concertticketingsystem.repository.CustomerRepository;
import com.example.concertticketingsystem.repository.LoyaltyCardRepository;
import com.example.concertticketingsystem.repository.TicketPurchaseRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Validated
public class TicketPurchaseService {

    private final TicketPurchaseRepository purchaseRepository;
    private final CustomerRepository customerRepository;
    private final LoyaltyCardRepository loyaltyCardRepository;
    private final ConcertAvailabilityGateway concertGateway;
    private final TicketPricingService pricingService;
    private final Clock clock;

    public TicketPurchaseService(
            TicketPurchaseRepository purchaseRepository,
            CustomerRepository customerRepository,
            LoyaltyCardRepository loyaltyCardRepository,
            ConcertAvailabilityGateway concertGateway,
            TicketPricingService pricingService,
            Clock clock) {

        this.purchaseRepository = purchaseRepository;
        this.customerRepository = customerRepository;
        this.loyaltyCardRepository = loyaltyCardRepository;
        this.concertGateway = concertGateway;
        this.pricingService = pricingService;
        this.clock = clock;
    }

    // Apres l'attente du verrou, SUM doit voir les achats qui viennent d'etre valides.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TicketPurchaseResponse purchase(
            @Valid TicketPurchaseRequest request) {

        if (request.concertId() == null ||
                request.customerId() == null ||
                request.quantity() == null ||
                request.quantity() < 1 ||
                request.quantity() > 6) {
            throw new IllegalArgumentException(
                    "Données d'achat invalides"
            );
        }

        Customer customer = customerRepository
                .findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CLIENT_INTROUVABLE", "Client introuvable"
                ));

        // Le gateway garde le verrou du concert jusqu'au commit de cet achat.
        ConcertAvailabilityGateway.ConcertInfo concert =
                concertGateway.findAndLockConcert(
                        request.concertId()
                );

        if (concert == null) {
            throw new ResourceNotFoundException(
                    "CONCERT_INTROUVABLE", "Concert introuvable"
            );
        }

        // A consulta deve ocorrer após obter o bloqueio.
        long sold = purchaseRepository
                .sumQuantityByConcertId(concert.id());

        long remaining = (long) concert.capacity() - sold;

        if (remaining < request.quantity()) {
            throw new BusinessConflictException(
                    "JAUGE_INSUFFISANTE", "Places insuffisantes pour cet achat."
            );
        }

        LocalDate today = LocalDate.now(clock);

        boolean activeCard = loyaltyCardRepository
                .existsByCustomerIdAndPurchasedAtLessThanEqualAndExpiresAtGreaterThan(
                        request.customerId(),
                        today,
                        today
                );

        BigDecimal total = pricingService.calculateTotal(
                concert.unitPrice(),
                request.quantity(),
                activeCard
        );

        BigDecimal discountRate =
                pricingService.discountRate(activeCard);

        String code = UUID.randomUUID().toString();

        TicketPurchase purchase = new TicketPurchase(
                code,
                customer,
                concert.id(),
                request.quantity(),
                total,
                LocalDateTime.now(clock)
        );

        purchaseRepository.saveAndFlush(purchase);

        return new TicketPurchaseResponse(
                code,
                request.quantity(),
                concert.unitPrice(),
                discountRate,
                total,
                Math.toIntExact(remaining - request.quantity())
        );
    }
}
