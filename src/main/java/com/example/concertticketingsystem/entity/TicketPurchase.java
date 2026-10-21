
package com.example.concertticketingsystem.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_purchases")
public class TicketPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // Referência temporária até à integração com Concert
    @Column(name = "concert_id", nullable = false)
    private Long concertId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "total_amount", nullable = false,
            precision = 10, scale = 2)
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
