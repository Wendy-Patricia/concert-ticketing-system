package com.example.concertticketingsystem.repository;

import com.example.concertticketingsystem.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    boolean existsByNameAndCity(String name, String city);
}
