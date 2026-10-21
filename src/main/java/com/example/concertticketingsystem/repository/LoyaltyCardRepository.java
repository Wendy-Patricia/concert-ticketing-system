
package com.example.concertticketingsystem.repository;

import com.example.concertticketingsystem.entity.LoyaltyCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface LoyaltyCardRepository
        extends JpaRepository<LoyaltyCard, Long> {

    boolean existsByCustomerIdAndPurchasedAtLessThanEqualAndExpiresAtGreaterThan(
            Long customerId,
            LocalDate purchaseDate,
            LocalDate expiryDate
    );
}
