
package com.example.concertticketingsystem.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "loyalty_cards")
public class LoyaltyCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "purchased_at", nullable = false)
    private LocalDate purchasedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDate expiresAt;

    public LoyaltyCard() {}

    public LoyaltyCard(Customer customer,
                       LocalDate purchasedAt,
                       LocalDate expiresAt) {
        this.customer = customer;
        this.purchasedAt = purchasedAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public LocalDate getPurchasedAt() {
        return purchasedAt;
    }

    public LocalDate getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive(LocalDate date) {
        return !date.isBefore(purchasedAt)
                && date.isBefore(expiresAt);
    }
}
