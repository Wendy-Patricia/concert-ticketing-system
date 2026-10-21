package com.example.concertticketingsystem;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class CatalogApiTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void createsArtistAndVenueWithGeneratedIds() throws Exception {
        mvc.perform(post("/api/production/artistes").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Nina Simone","musicalGenre":"Jazz"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Nina Simone"))
                .andExpect(jsonPath("$.musicalGenre").value("Jazz"));
        mvc.perform(post("/api/admin/salles").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Olympia","city":"Paris","capacity":500}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.city").value("Paris"))
                .andExpect(jsonPath("$.capacity").value(500));
    }

    @Test
    void createsAndReadsConcertWithNestedDtosAndLocation() throws Exception {
        long artist = artist("Nina Simone");
        long venue = venue("Olympia", "Paris");
        String date = day().atTime(20, 30).toString();
        MvcResult result = mvc.perform(post("/api/production/concerts").contentType(MediaType.APPLICATION_JSON)
                        .content(concertJson(artist, venue, date, "42.50")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.artist.id").value(artist))
                .andExpect(jsonPath("$.venue.id").value(venue))
                .andExpect(jsonPath("$.unitPrice").value(42.50))
                .andReturn();
        long id = id(result);
        mvc.perform(get("/api/concerts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.artist.name").value("Nina Simone"))
                .andExpect(jsonPath("$.venue.name").value("Olympia"))
                .andExpect(jsonPath("$.title").value("Jazz Night"));
        org.assertj.core.api.Assertions.assertThat(result.getResponse().getHeader("Location"))
                .isEqualTo("/api/concerts/" + id);
    }

    @Test
    void listsEmptyCatalog() throws Exception {
        mvc.perform(get("/api/concerts"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listsAllConcertsInDateOrder() throws Exception {
        long artist = artist("Nina Simone");
        long venue = venue("Olympia", "Paris");
        long later = concert(artist, venue, day().plusDays(1).atStartOfDay());
        long earlier = concert(artist, venue, day().atStartOfDay());
        mvc.perform(get("/api/concerts"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(earlier))
                .andExpect(jsonPath("$[1].id").value(later));
    }

    @Test
    void combinesInclusiveDateAndArtistFilters() throws Exception {
        long firstArtist = artist("Nina Simone");
        long secondArtist = artist("Miles Davis");
        long venue = venue("Olympia", "Paris");
        concert(firstArtist, venue, day().minusDays(1).atTime(23, 59));
        long first = concert(firstArtist, venue, day().atStartOfDay());
        long last = concert(firstArtist, venue, day().atTime(23, 59, 59, 999_999_000));
        concert(firstArtist, venue, day().plusDays(1).atStartOfDay());
        concert(secondArtist, venue, day().atTime(12, 0));

        mvc.perform(get("/api/concerts").param("du", day().toString())
                        .param("au", day().toString()).param("artiste", Long.toString(firstArtist)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(first))
                .andExpect(jsonPath("$[1].id").value(last));
    }

    @ParameterizedTest
    @ValueSource(strings = {"du", "au", "artiste"})
    void acceptsEachFilterIndependently(String filter) throws Exception {
        long firstArtist = artist("Nina Simone");
        long secondArtist = artist("Miles Davis");
        long venue = venue("Olympia", "Paris");
        long first = concert(firstArtist, venue, day().atStartOfDay());
        long second = concert(secondArtist, venue, day().plusDays(1).atStartOfDay());
        String value = filter.equals("artiste") ? Long.toString(firstArtist) : day().toString();
        long expectedId = filter.equals("du") ? second : first;
        if (filter.equals("du")) {
            value = day().plusDays(1).toString();
        }
        mvc.perform(get("/api/concerts").param(filter, value))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(expectedId));
    }

    @Test
    void returnsEmptyListForUnknownArtistFilter() throws Exception {
        mvc.perform(get("/api/concerts").param("artiste", Long.toString(Long.MAX_VALUE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rejectsReversedDateRange() throws Exception {
        mvc.perform(get("/api/concerts").param("du", day().plusDays(1).toString())
                        .param("au", day().toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ECHOUEE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"du=invalid", "au=2026-02-30", "artiste=abc", "artiste=0", "artiste=-1"})
    void rejectsMalformedOrInvalidFilters(String query) throws Exception {
        String[] parts = query.split("=");
        mvc.perform(get("/api/concerts").param(parts[0], parts[1]))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ECHOUEE"));
    }

    @Test
    void returnsStructuredNotFoundForUnknownConcert() throws Exception {
        mvc.perform(get("/api/concerts/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("CONCERT_INTROUVABLE"))
                .andExpect(jsonPath("$.message").value("Concert introuvable."))
                .andExpect(jsonPath("$.path").value("/api/concerts/" + Long.MAX_VALUE))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc"})
    void rejectsInvalidConcertPathId(String id) throws Exception {
        mvc.perform(get("/api/concerts/{id}", id)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ECHOUEE"));
    }

    @Test
    void rejectsDuplicateArtistAndVenueWithConflict() throws Exception {
        artist("Nina Simone");
        mvc.perform(post("/api/production/artistes").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Nina Simone","musicalGenre":"Blues"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESSOURCE_EXISTANTE"));
        venue("Olympia", "Paris");
        mvc.perform(post("/api/admin/salles").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Olympia","city":"Paris","capacity":600}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESSOURCE_EXISTANTE"));
        venue("Olympia", "Lyon");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":null}", "{\"name\":\"  \"}"})
    void rejectsMissingArtistName(String json) throws Exception {
        mvc.perform(post("/api/production/artistes").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ECHOUEE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"49", "50001", "null"})
    void rejectsInvalidVenueCapacity(String capacity) throws Exception {
        mvc.perform(post("/api/admin/salles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Olympia\",\"city\":\"Paris\",\"capacity\":" + capacity + "}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ECHOUEE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "city"})
    void rejectsMissingVenueNameOrCity(String field) throws Exception {
        String json = field.equals("name")
                ? "{\"city\":\"Paris\",\"capacity\":500}"
                : "{\"name\":\"Olympia\",\"capacity\":500}";
        mvc.perform(post("/api/admin/salles").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ECHOUEE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "null", "0.001", "100000000.00"})
    void rejectsInvalidConcertPrice(String price) throws Exception {
        mvc.perform(post("/api/production/concerts").contentType(MediaType.APPLICATION_JSON)
                        .content(concertJson(1L, 1L, day().atStartOfDay().toString(), price)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ECHOUEE"));
    }

    @Test
    void rejectsPastDateAndMissingConcertFields() throws Exception {
        mvc.perform(post("/api/production/concerts").contentType(MediaType.APPLICATION_JSON)
                        .content(concertJson(1L, 1L, LocalDateTime.now().minusDays(1).toString(), "42.50")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/production/concerts").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ECHOUEE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"artistId", "venueId"})
    void rejectsMissingAssociationId(String field) throws Exception {
        String json = concertJson(1L, 1L, day().atStartOfDay().toString(), "42.50")
                .replace("\"" + field + "\":1", "\"" + field + "\":null");
        mvc.perform(post("/api/production/concerts").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ECHOUEE"));
    }

    @Test
    void rejectsUnknownArtistWithNotFound() throws Exception {
        long venue = venue("Olympia", "Paris");
        mvc.perform(post("/api/production/concerts").contentType(MediaType.APPLICATION_JSON)
                        .content(concertJson(Long.MAX_VALUE, venue, day().atStartOfDay().toString(), "42.50")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("ARTISTE_INTROUVABLE"));
    }

    @Test
    void rejectsUnknownVenueWithNotFound() throws Exception {
        long artist = artist("Nina Simone");
        mvc.perform(post("/api/production/concerts").contentType(MediaType.APPLICATION_JSON)
                        .content(concertJson(artist, Long.MAX_VALUE, day().atStartOfDay().toString(), "42.50")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SALLE_INTROUVABLE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/production/artistes", "/api/admin/salles", "/api/production/concerts"})
    void rejectsMalformedJson(String path) throws Exception {
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ECHOUEE"));
    }

    private long artist(String name) throws Exception {
        return createdId("/api/production/artistes", "{\"name\":\"" + name + "\",\"musicalGenre\":\"Jazz\"}");
    }

    private long venue(String name, String city) throws Exception {
        return createdId("/api/admin/salles", "{\"name\":\"" + name + "\",\"city\":\"" + city
                + "\",\"capacity\":500}");
    }

    private long concert(long artist, long venue, LocalDateTime date) throws Exception {
        return createdId("/api/production/concerts", concertJson(artist, venue, date.toString(), "42.50"));
    }

    private long createdId(String path, String json) throws Exception {
        return id(mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn());
    }

    private long id(MvcResult result) throws Exception {
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private LocalDate day() {
        return LocalDate.now().plusDays(30);
    }

    private String concertJson(long artist, long venue, String date, String price) {
        return "{\"title\":\"Jazz Night\",\"artistId\":" + artist + ",\"venueId\":" + venue
                + ",\"dateTime\":\"" + date + "\",\"unitPrice\":" + price + "}";
    }
}
