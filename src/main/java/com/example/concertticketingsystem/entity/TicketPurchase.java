
package com.example.concertticketingsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_purchases")
public class TicketPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // L'identifiant reste l'entree du contrat de billetterie existant.
    @Column(name = "concert_id", nullable = false)
    private Long concertId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concert_id", insertable = false, updatable = false, nullable = false)
    private Concert concert;

    @NotNull
    @Min(1)
    @Max(6)
    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "total_amount", nullable = false,
            precision = 12, scale = 2)
    @NotNull
    @Positive
    @Digits(integer = 10, fraction = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TicketPurchase() {}

    public TicketPurchase(
            String code,
            Customer customer,
            Long concertId,
            Integer quantity,
            BigDecimal totalAmount,
            LocalDateTime createdAt) {

        this.code = code;
        this.customer = customer;
        this.concertId = concertId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Long getConcertId() {
        return concertId;
    }

    public Concert getConcert() {
        return concert;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
