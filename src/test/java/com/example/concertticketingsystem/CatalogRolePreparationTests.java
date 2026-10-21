package com.example.concertticketingsystem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.concertticketingsystem.config.ClockConfig;
import com.example.concertticketingsystem.config.SecurityConfig;
import com.example.concertticketingsystem.controller.ArtistController;
import com.example.concertticketingsystem.controller.ConcertController;
import com.example.concertticketingsystem.controller.VenueController;
import com.example.concertticketingsystem.dto.ArtistResponse;
import com.example.concertticketingsystem.dto.ConcertResponse;
import com.example.concertticketingsystem.dto.VenueResponse;
import com.example.concertticketingsystem.service.ArtistService;
import com.example.concertticketingsystem.service.ConcertService;
import com.example.concertticketingsystem.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ArtistController.class, VenueController.class, ConcertController.class})
@Import({ClockConfig.class, SecurityConfig.class})
class CatalogRolePreparationTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ArtistService artists;

    @MockitoBean
    private VenueService venues;

    @MockitoBean
    private ConcertService concerts;

    @BeforeEach
    void responses() {
        ArtistResponse artist = new ArtistResponse(1L, "Nina Simone", "Jazz");
        VenueResponse venue = new VenueResponse(1L, "Olympia", "Paris", 500);
        ConcertResponse concert = new ConcertResponse(1L, "Jazz Night", artist, venue,
                LocalDateTime.now().plusDays(30), new BigDecimal("42.50"));
        given(artists.create(any())).willReturn(artist);
        given(venues.create(any())).willReturn(venue);
        given(concerts.create(any())).willReturn(concert);
        given(concerts.get(1L)).willReturn(concert);
        given(concerts.list(null, null, null)).willReturn(List.of(concert));
    }

    @Test
    void preparesPublicReadAccess() throws Exception {
        mvc.perform(get("/api/concerts")).andExpect(status().isOk());
        mvc.perform(get("/api/concerts/1")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void clientCanReadButCannotCreateCatalogResources() throws Exception {
        mvc.perform(get("/api/concerts")).andExpect(status().isOk());
        mvc.perform(post("/api/production/artistes").contentType(MediaType.APPLICATION_JSON).content(artistJson()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/salles").contentType(MediaType.APPLICATION_JSON).content(venueJson()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/production/concerts").contentType(MediaType.APPLICATION_JSON).content(concertJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PRODUCTEUR")
    void producerCanCreateArtistsAndConcertsButCannotCreateVenues() throws Exception {
        mvc.perform(post("/api/production/artistes").contentType(MediaType.APPLICATION_JSON).content(artistJson()))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/production/concerts").contentType(MediaType.APPLICATION_JSON).content(concertJson()))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/admin/salles").contentType(MediaType.APPLICATION_JSON).content(venueJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateAllCatalogResources() throws Exception {
        mvc.perform(post("/api/production/artistes").contentType(MediaType.APPLICATION_JSON).content(artistJson()))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/admin/salles").contentType(MediaType.APPLICATION_JSON).content(venueJson()))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/production/concerts").contentType(MediaType.APPLICATION_JSON).content(concertJson()))
                .andExpect(status().isCreated());
    }

    @Test
    void anonymousCannotCreateWhenMethodSecurityIsEnabled() throws Exception {
        mvc.perform(post("/api/production/artistes").contentType(MediaType.APPLICATION_JSON).content(artistJson()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(artists);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void mapsDatabaseIntegrityConflictWithoutLeakingSql() throws Exception {
        given(artists.create(any())).willThrow(new DataIntegrityViolationException("INSERT secret SQL"));
        mvc.perform(post("/api/production/artistes").contentType(MediaType.APPLICATION_JSON).content(artistJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLIT_INTEGRITE"))
                .andExpect(content().string(not(containsString("INSERT"))));
    }

    private String artistJson() {
        return "{\"name\":\"Nina Simone\",\"musicalGenre\":\"Jazz\"}";
    }

    private String venueJson() {
        return "{\"name\":\"Olympia\",\"city\":\"Paris\",\"capacity\":500}";
    }

    private String concertJson() {
        return "{\"title\":\"Jazz Night\",\"artistId\":1,\"venueId\":1,\"dateTime\":\""
                + LocalDateTime.now().plusDays(30) + "\",\"unitPrice\":42.50}";
    }

}
