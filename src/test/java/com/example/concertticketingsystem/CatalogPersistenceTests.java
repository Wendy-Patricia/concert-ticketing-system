package com.example.concertticketingsystem;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import com.example.concertticketingsystem.entity.Artist;
import com.example.concertticketingsystem.entity.Concert;
import com.example.concertticketingsystem.entity.Venue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class CatalogPersistenceTests {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void appliesCatalogMigrationBeforeHibernateValidation() {
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1",
                Integer.class)).isEqualTo(1);
        assertThat(entityManager.getEntityManagerFactory().getProperties())
                .containsEntry("hibernate.hbm2ddl.auto", "validate");
    }

    @ParameterizedTest
    @ValueSource(ints = {50, 50_000})
    void persistsVenueAtCapacityLimits(int capacity) {
        Venue venue = new Venue("Olympia", "Paris", capacity);
        entityManager.persist(venue);
        entityManager.flush();
        entityManager.clear();

        assertThat(venue.getId()).isNotNull();
        Venue loaded = entityManager.find(Venue.class, venue.getId());
        assertThat(loaded.getName()).isEqualTo("Olympia");
        assertThat(loaded.getCity()).isEqualTo("Paris");
        assertThat(loaded.getCapacity()).isEqualTo(capacity);
    }

    @Test
    void persistsConcertAndReloadsBothAssociations() {
        Artist artist = persistArtist();
        Venue venue = persistVenue();
        LocalDateTime date = futureDate();
        Concert concert = new Concert("Jazz Night", artist, venue, date, new BigDecimal("42.50"));
        entityManager.persist(concert);
        entityManager.flush();
        entityManager.clear();

        assertThat(concert.getId()).isNotNull();
        Concert loaded = entityManager.find(Concert.class, concert.getId());
        assertThat(loaded.getTitle()).isEqualTo("Jazz Night");
        assertThat(loaded.getDateTime()).isEqualTo(date);
        assertThat(loaded.getUnitPrice()).isEqualByComparingTo("42.50");
        assertThat(loaded.getArtist().getId()).isEqualTo(artist.getId());
        assertThat(loaded.getArtist().getName()).isEqualTo("Nina Simone");
        assertThat(loaded.getArtist().getMusicalGenre()).isEqualTo("Jazz");
        assertThat(loaded.getVenue().getId()).isEqualTo(venue.getId());
        assertThat(loaded.getVenue().getCity()).isEqualTo("Paris");
    }

    @ParameterizedTest
    @ValueSource(ints = {49, 50_001})
    void rejectsInvalidCapacityBeforeInsert(int capacity) {
        assertValidationFailure(new Venue("Olympia", "Paris", capacity), "capacity");
    }

    @Test
    void rejectsMissingCapacity() {
        assertValidationFailure(new Venue("Olympia", "Paris", null), "capacity");
    }

    @ParameterizedTest
    @MethodSource("missingVenueNamesOrCities")
    void rejectsMissingVenueNameOrCity(String name, String city, String property) {
        assertValidationFailure(new Venue(name, city, 500), property);
    }

    static Stream<Arguments> missingVenueNamesOrCities() {
        return Stream.of(
                Arguments.of(null, "Paris", "name"),
                Arguments.of("", "Paris", "name"),
                Arguments.of(" \t", "Paris", "name"),
                Arguments.of("Olympia", null, "city"),
                Arguments.of("Olympia", "", "city"),
                Arguments.of("Olympia", " \t", "city"));
    }

    @Test
    void rejectsDuplicateVenueInSameCity() {
        persistVenue();
        assertThatThrownBy(() -> {
            entityManager.persist(new Venue("Olympia", "Paris", 600));
            entityManager.flush();
        }).isInstanceOf(org.hibernate.exception.ConstraintViolationException.class)
                .hasMessageContaining("uk_venues_name_city");
    }

    @Test
    void allowsSameVenueNameInDifferentCitiesAndDifferentNamesInSameCity() {
        persistVenue();
        entityManager.persist(new Venue("Olympia", "Lyon", 600));
        entityManager.persist(new Venue("Zenith", "Paris", 700));
        entityManager.flush();
        entityManager.clear();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM venues", Integer.class)).isEqualTo(3);
    }

    @Test
    void rejectsDuplicateArtistNameInMySql() {
        persistArtist();
        assertThatThrownBy(() -> {
            entityManager.persist(new Artist("Nina Simone", "Blues"));
            entityManager.flush();
        }).isInstanceOf(org.hibernate.exception.ConstraintViolationException.class)
                .hasMessageContaining("uk_artists_name");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-0.01", "0.001", "100000000.00"})
    void rejectsInvalidConcertPrice(String price) {
        Concert concert = new Concert("Jazz Night", persistArtist(), persistVenue(), futureDate(),
                price == null ? null : new BigDecimal(price));
        assertValidationFailure(concert, "unitPrice");
    }

    @ParameterizedTest
    @ValueSource(strings = {"artist", "venue"})
    void rejectsMissingConcertAssociation(String missingAssociation) {
        Artist artist = missingAssociation.equals("artist") ? null : persistArtist();
        Venue venue = missingAssociation.equals("venue") ? null : persistVenue();
        assertValidationFailure(new Concert("Jazz Night", artist, venue, futureDate(),
                new BigDecimal("42.50")), missingAssociation);
    }

    @Test
    void rejectsPastConcertDate() {
        assertValidationFailure(new Concert("Jazz Night", persistArtist(), persistVenue(),
                LocalDateTime.now().minusDays(1), new BigDecimal("42.50")), "dateTime");
    }

    @Test
    void rejectsMissingConcertDate() {
        assertValidationFailure(new Concert("Jazz Night", persistArtist(), persistVenue(),
                null, new BigDecimal("42.50")), "dateTime");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " \t"})
    void rejectsMissingConcertTitle(String title) {
        assertValidationFailure(new Concert(title, persistArtist(), persistVenue(), futureDate(),
                new BigDecimal("42.50")), "title");
    }

    @ParameterizedTest
    @ValueSource(ints = {49, 50_001})
    void databaseAlsoRejectsInvalidCapacity(int capacity) {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO venues (name, city, capacity) VALUES (?, ?, ?)", "Olympia", "Paris", capacity))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_venues_capacity")
                .rootCause().isInstanceOfSatisfying(SQLException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(3819));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-0.01"})
    void databaseAlsoRejectsNonPositivePrice(String price) {
        Artist artist = persistArtist();
        Venue venue = persistVenue();
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO concerts (title, artist_id, venue_id, date_time, unit_price) VALUES (?, ?, ?, ?, ?)",
                "Jazz Night", artist.getId(), venue.getId(), futureDate(), new BigDecimal(price)))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_concerts_unit_price")
                .rootCause().isInstanceOfSatisfying(SQLException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(3819));
    }

    @ParameterizedTest
    @ValueSource(strings = {"artist", "venue"})
    void databaseAlsoRejectsNullAssociation(String missingAssociation) {
        Artist artist = persistArtist();
        Venue venue = persistVenue();
        Long artistId = missingAssociation.equals("artist") ? null : artist.getId();
        Long venueId = missingAssociation.equals("venue") ? null : venue.getId();
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO concerts (title, artist_id, venue_id, date_time, unit_price) VALUES (?, ?, ?, ?, ?)",
                "Jazz Night", artistId, venueId, futureDate(), new BigDecimal("42.50")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(missingAssociation + "_id");
    }

    @ParameterizedTest
    @ValueSource(strings = {"artist", "venue"})
    void databaseRejectsReferenceToNonexistentAssociation(String missingAssociation) {
        Artist artist = persistArtist();
        Venue venue = persistVenue();
        Long artistId = missingAssociation.equals("artist") ? Long.MAX_VALUE : artist.getId();
        Long venueId = missingAssociation.equals("venue") ? Long.MAX_VALUE : venue.getId();
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO concerts (title, artist_id, venue_id, date_time, unit_price) VALUES (?, ?, ?, ?, ?)",
                "Jazz Night", artistId, venueId, futureDate(), new BigDecimal("42.50")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_concerts_" + missingAssociation);
    }

    private Artist persistArtist() {
        Artist artist = new Artist("Nina Simone", "Jazz");
        entityManager.persist(artist);
        return artist;
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "city"})
    void databaseRejectsBlankVenueNamesAndCities(String field) {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO venues (name, city, capacity) VALUES (?, ?, ?)",
                field.equals("name") ? " " : "Olympia", field.equals("city") ? " " : "Paris", 500))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_venues_" + field + "_not_blank")
                .rootCause().isInstanceOfSatisfying(SQLException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(3819));
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "city", "capacity"})
    void databaseRejectsNullMandatoryVenueFields(String field) {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO venues (name, city, capacity) VALUES (?, ?, ?)",
                field.equals("name") ? null : "Olympia", field.equals("city") ? null : "Paris",
                field.equals("capacity") ? null : 500))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining(field);
    }

    @ParameterizedTest
    @ValueSource(strings = {"artist", "venue"})
    void databasePreventsDeletingReferencedCatalogEntities(String reference) {
        Artist artist = persistArtist();
        Venue venue = persistVenue();
        entityManager.persist(new Concert("Jazz Night", artist, venue, futureDate(), new BigDecimal("42.50")));
        entityManager.flush();
        String table = reference.equals("artist") ? "artists" : "venues";
        Long id = reference.equals("artist") ? artist.getId() : venue.getId();
        assertThatThrownBy(() -> jdbc.update("DELETE FROM " + table + " WHERE id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_concerts_" + reference);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "99999999.99"})
    void persistsExactPriceAtDecimalLimits(String price) {
        Concert concert = new Concert("Jazz Night", persistArtist(), persistVenue(), futureDate(),
                new BigDecimal(price));
        entityManager.persist(concert);
        entityManager.flush();
        entityManager.clear();
        assertThat(entityManager.find(Concert.class, concert.getId()).getUnitPrice()).isEqualByComparingTo(price);
    }

    private Venue persistVenue() {
        Venue venue = new Venue("Olympia", "Paris", 500);
        entityManager.persist(venue);
        return venue;
    }

    private LocalDateTime futureDate() {
        return LocalDateTime.now().plusDays(7).withNano(0);
    }

    private void assertValidationFailure(Object entity, String property) {
        assertThatThrownBy(() -> {
            entityManager.persist(entity);
            entityManager.flush();
        }).isInstanceOfSatisfying(ConstraintViolationException.class, exception ->
                assertThat(exception.getConstraintViolations())
                        .extracting(violation -> violation.getPropertyPath().toString())
                        .contains(property));
    }
}
