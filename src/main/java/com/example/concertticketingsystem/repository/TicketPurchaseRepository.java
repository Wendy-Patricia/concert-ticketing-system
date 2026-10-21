
package com.example.concertticketingsystem.repository;

import com.example.concertticketingsystem.entity.TicketPurchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketPurchaseRepository
        extends JpaRepository<TicketPurchase, Long> {

    boolean existsByCode(String code);

    @Query("""
           SELECT COALESCE(SUM(p.quantity), 0)
           FROM TicketPurchase p
           WHERE p.concertId = :concertId
           """)
    Long sumQuantityByConcertId(
            @Param("concertId") Long concertId
    );
}
