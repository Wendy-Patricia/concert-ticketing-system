
package com.example.concertticketingsystem.service;

import java.math.BigDecimal;

public interface ConcertAvailabilityGateway {

    ConcertInfo findAndLockConcert(Long concertId);

    record ConcertInfo(
            Long id,
            BigDecimal unitPrice,
            int capacity
    ) {}
}
