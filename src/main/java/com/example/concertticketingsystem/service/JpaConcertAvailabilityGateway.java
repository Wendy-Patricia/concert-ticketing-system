package com.example.concertticketingsystem.service;

import java.time.Clock;
import java.time.LocalDateTime;

import com.example.concertticketingsystem.entity.Concert;
import com.example.concertticketingsystem.exception.BusinessConflictException;
import com.example.concertticketingsystem.exception.ResourceNotFoundException;
import com.example.concertticketingsystem.repository.ConcertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JpaConcertAvailabilityGateway implements ConcertAvailabilityGateway {
    private final ConcertRepository concerts;
    private final Clock clock;

    public JpaConcertAvailabilityGateway(ConcertRepository concerts, Clock clock) {
        this.concerts = concerts;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ConcertInfo findAndLockConcert(Long concertId) {
        Concert concert = concerts.findByIdForUpdate(concertId).orElseThrow(() ->
                new ResourceNotFoundException("CONCERT_INTROUVABLE", "Concert introuvable."));
        if (!concert.getDateTime().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessConflictException("CONCERT_TERMINE", "Ce concert n'est plus disponible à la vente.");
        }
        return new ConcertInfo(concert.getId(), concert.getUnitPrice(), concert.getVenue().getCapacity());
    }
}
