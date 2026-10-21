package com.example.concertticketingsystem;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import com.example.concertticketingsystem.dto.request.TicketPurchaseRequest;
import com.example.concertticketingsystem.dto.response.LoyaltyCardResponse;
import com.example.concertticketingsystem.dto.response.TicketPurchaseResponse;
import com.example.concertticketingsystem.entity.*;
import com.example.concertticketingsystem.exception.BusinessConflictException;
import com.example.concertticketingsystem.repository.ConcertRepository;
import com.example.concertticketingsystem.service.ConcertAvailabilityGateway;
import com.example.concertticketingsystem.service.LoyaltyCardService;
import com.example.concertticketingsystem.service.TicketPurchaseService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TicketingConcurrencyTests {
    @Autowired private TicketPurchaseService purchases;
    @Autowired private LoyaltyCardService cards;
    @Autowired private ConcertRepository concerts;
    @Autowired private ConcertAvailabilityGateway gateway;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Clock clock;
    @PersistenceContext private EntityManager em;
    private Fixture fixture;

    // Aucune transaction de test englobante : les threads lisent des donnees commitees.
    @RepeatedTest(3)
    void onlyOneOfTwoConcurrentPurchasesOfFourSucceedsWithFiveSeatsLeft() throws Exception {
        fixture = seed(45);
        List<Attempt<TicketPurchaseResponse>> attempts = simultaneously(
                () -> purchases.purchase(new TicketPurchaseRequest(fixture.concert(), fixture.firstCustomer(), 4)),
                () -> purchases.purchase(new TicketPurchaseRequest(fixture.concert(), fixture.secondCustomer(), 4)));
        assertThat(attempts).filteredOn(attempt -> attempt.failure() == null).hasSize(1)
                .allSatisfy(attempt -> assertThat(attempt.value().remainingSeats()).isEqualTo(1));
        assertThat(attempts).filteredOn(attempt -> attempt.failure() != null).hasSize(1)
                .allSatisfy(attempt -> assertThat(attempt.failure())
                        .isInstanceOfSatisfying(BusinessConflictException.class,
                                error -> assertThat(error.getCode()).isEqualTo("JAUGE_INSUFFISANTE")));
        assertThat(jdbc.queryForObject("SELECT SUM(quantity) FROM ticket_purchases WHERE concert_id=?",
                Long.class, fixture.concert())).isEqualTo(49);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ticket_purchases WHERE concert_id=?",
                Long.class, fixture.concert())).isEqualTo(9);
    }

    @Test
    void concurrentCardRequestsCreateOnlyOneActiveCard() throws Exception {
        fixture = seed(0);
        List<Attempt<LoyaltyCardResponse>> attempts = simultaneously(
                () -> cards.purchaseCard(fixture.firstCustomer()),
                () -> cards.purchaseCard(fixture.firstCustomer()));
        assertThat(attempts).filteredOn(attempt -> attempt.failure() == null).hasSize(1);
        assertThat(attempts).filteredOn(attempt -> attempt.failure() != null).hasSize(1)
                .allSatisfy(attempt -> assertThat(attempt.failure())
                        .isInstanceOfSatisfying(BusinessConflictException.class,
                                error -> assertThat(error.getCode()).isEqualTo("CARTE_DEJA_ACTIVE")));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM loyalty_cards WHERE customer_id=?",
                Long.class, fixture.firstCustomer())).isEqualTo(1);
    }

    @Test
    void purchaseWaitsForPessimisticConcertLockUntilOtherTransactionCommits() throws Exception {
        fixture = seed(0);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> holder = executor.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                concerts.findByIdForUpdate(fixture.concert()).orElseThrow();
                locked.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Lock holder timed out");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
                return null;
            }));
            assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
            Future<TicketPurchaseResponse> buyer = executor.submit(() -> purchases.purchase(
                    new TicketPurchaseRequest(fixture.concert(), fixture.firstCustomer(), 1)));
            assertThatThrownBy(() -> buyer.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown();
            holder.get(10, TimeUnit.SECONDS);
            assertThat(buyer.get(10, TimeUnit.SECONDS).remainingSeats()).isEqualTo(49);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void gatewayCannotAcquireLockOutsidePurchaseTransaction() {
        assertThatThrownBy(() -> gateway.findAndLockConcert(1L))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private Fixture seed(int sold) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            String suffix = UUID.randomUUID().toString();
            Artist artist = new Artist("Concurrent " + suffix, "Jazz");
            Venue venue = new Venue("Concurrent " + suffix, "Paris", 50);
            em.persist(artist);
            em.persist(venue);
            Concert concert = new Concert("Concurrent", artist, venue, LocalDateTime.now(clock).plusDays(30),
                    new BigDecimal("50.00"));
            em.persist(concert);
            Customer first = new Customer("Alice", "a-" + suffix + "@example.com", new Address("Rue", "Paris"));
            Customer second = new Customer("Bob", "b-" + suffix + "@example.com", new Address("Rue", "Paris"));
            em.persist(first);
            em.persist(second);
            for (int remaining = sold; remaining > 0;) {
                int quantity = Math.min(6, remaining);
                em.persist(new TicketPurchase(UUID.randomUUID().toString(), first, concert.getId(), quantity,
                        new BigDecimal("50.00").multiply(BigDecimal.valueOf(quantity)), LocalDateTime.now(clock)));
                remaining -= quantity;
            }
            em.flush();
            return new Fixture(artist.getId(), venue.getId(), concert.getId(), first.getId(), second.getId());
        });
    }

    @AfterEach
    void removeCommittedFixturesOnly() {
        if (fixture != null) {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                jdbc.update("DELETE FROM ticket_purchases WHERE concert_id=?", fixture.concert());
                jdbc.update("DELETE FROM loyalty_cards WHERE customer_id IN (?,?)", fixture.firstCustomer(), fixture.secondCustomer());
                jdbc.update("DELETE FROM concerts WHERE id=?", fixture.concert());
                jdbc.update("DELETE FROM customers WHERE id IN (?,?)", fixture.firstCustomer(), fixture.secondCustomer());
                jdbc.update("DELETE FROM venues WHERE id=?", fixture.venue());
                jdbc.update("DELETE FROM artists WHERE id=?", fixture.artist());
            });
        }
    }

    private <T> List<Attempt<T>> simultaneously(Callable<T> first, Callable<T> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Attempt<T>> one = executor.submit(task(first, ready, start));
            Future<Attempt<T>> two = executor.submit(task(second, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        }
    }

    private <T> Callable<Attempt<T>> task(Callable<T> call, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent start timed out");
            }
            try {
                return new Attempt<>(call.call(), null);
            } catch (RuntimeException failure) {
                return new Attempt<>(null, failure);
            }
        };
    }

    private record Fixture(Long artist, Long venue, Long concert, Long firstCustomer, Long secondCustomer) { }
    private record Attempt<T>(T value, RuntimeException failure) { }
}
