
package com.example.concertticketingsystem.service;

import com.example.concertticketingsystem.dto.response.LoyaltyCardResponse;
import com.example.concertticketingsystem.entity.Customer;
import com.example.concertticketingsystem.entity.LoyaltyCard;
import com.example.concertticketingsystem.exception.BusinessConflictException;
import com.example.concertticketingsystem.exception.ResourceNotFoundException;
import com.example.concertticketingsystem.repository.CustomerRepository;
import com.example.concertticketingsystem.repository.LoyaltyCardRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class LoyaltyCardService {

    private final LoyaltyCardRepository loyaltyCardRepository;
    private final CustomerRepository customerRepository;
    private final Clock clock;

    public LoyaltyCardService(
            LoyaltyCardRepository loyaltyCardRepository,
            CustomerRepository customerRepository,
            Clock clock) {

        this.loyaltyCardRepository = loyaltyCardRepository;
        this.customerRepository = customerRepository;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LoyaltyCardResponse purchaseCard(Long customerId) {

        Customer customer = customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "CLIENT_INTROUVABLE", "Client introuvable : " + customerId
                        )
                );

        LocalDate today = LocalDate.now(clock);

        boolean hasActiveCard = loyaltyCardRepository
                .existsByCustomerIdAndPurchasedAtLessThanEqualAndExpiresAtGreaterThan(
                        customerId, today, today
                );

        if (hasActiveCard) {
            throw new BusinessConflictException(
                    "CARTE_DEJA_ACTIVE", "Le client possède déjà une carte de fidélité active"
            );
        }

        LoyaltyCard card = new LoyaltyCard(
                customer,
                today,
                today.plusYears(1)
        );

        LoyaltyCard saved = loyaltyCardRepository.save(card);

        return new LoyaltyCardResponse(
                saved.getId(),
                customerId,
                saved.getPurchasedAt(),
                saved.getExpiresAt()
        );
    }
}
