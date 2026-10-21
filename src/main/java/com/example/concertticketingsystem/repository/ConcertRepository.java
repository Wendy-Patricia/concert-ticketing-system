package com.example.concertticketingsystem.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.example.concertticketingsystem.entity.Concert;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

    @Override
    @EntityGraph(attributePaths = {"artist", "venue"})
    Optional<Concert> findById(Long id);

    @Query("""
            select c from Concert c
            join fetch c.artist a
            join fetch c.venue
            where (:fromDate is null or c.dateTime >= :fromDate)
              and (:toDate is null or c.dateTime <= :toDate)
              and (:artistId is null or a.id = :artistId)
            order by c.dateTime, c.id
            """)
    List<Concert> search(@Param("fromDate") LocalDateTime fromDate,
                         @Param("toDate") LocalDateTime toDate,
                         @Param("artistId") Long artistId);
}
