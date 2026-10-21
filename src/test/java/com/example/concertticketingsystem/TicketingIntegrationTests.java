package com.example.concertticketingsystem;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.concertticketingsystem.entity.*;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.web.csrf.CsrfToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"DEMO_PRODUCTEUR_PASSWORD=ProducteurDemo123!", "DEMO_ADMIN_PASSWORD=AdminDemo123!",
        "DEMO_CLIENT_PASSWORD=ClientDemo123!"})
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
class TicketingIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private Clock clock;
    @Autowired private JdbcTemplate jdbc;
    @PersistenceContext private EntityManager em;

    @Test
    void migratesBothModulesAndCreatesPublicCustomerWithEmbeddedAddress() throws Exception {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success=1 AND version IN ('1','2','3')",
                Integer.class)).isEqualTo(3);
        long id = register();
        em.flush();
        em.clear();
        Customer saved = em.find(Customer.class, id);
        assertThat(saved.getName()).isEqualTo("Alice");
        assertThat(saved.getAddress().getCity()).isEqualTo("Nancy");
        assertThat(saved.getAddress().getStreet()).isEqualTo("Rue de Paris");
    }

    @Test
    void returnsStructuredConflictForDuplicateCustomerEmail() throws Exception {
        register();
        mvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content(customerJson()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("RESSOURCE_EXISTANTE"))
                .andExpect(jsonPath("$.path").value("/api/clients"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":\"Alice\",\"email\":\"invalid\",\"street\":\"Rue\",\"city\":\"Nancy\"}"})
    void rejectsInvalidCustomerBody(String body) throws Exception {
        mvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ECHOUEE"));
    }

    @Test
    void createsOneYearCardAndRejectsSecondActiveCard() throws Exception {
        long customer = register();
        String path = "/api/clients/" + customer + "/carte-fidelite";
        LocalDate today = LocalDate.now(clock);
        mvc.perform(post(path).with(httpBasic("admin", "AdminDemo123!")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.customerId").value(customer))
                .andExpect(jsonPath("$.purchasedAt").value(today.toString()))
                .andExpect(jsonPath("$.expiresAt").value(today.plusYears(1).toString()));
        mvc.perform(post(path).with(httpBasic("client", "ClientDemo123!")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CARTE_DEJA_ACTIVE"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM loyalty_cards", Integer.class)).isEqualTo(1);
    }

    @Test
    void appliesDiscountAndPersistsPurchaseWithBothJpaRelations() throws Exception {
        long customer = register();
        Concert concert = catalog(new BigDecimal("50.00"));
        mvc.perform(post("/api/clients/{id}/carte-fidelite", customer).with(httpBasic("client", "ClientDemo123!")))
                .andExpect(status().isCreated());
        MvcResult result = purchase(customer, concert.getId(), 2)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.totalAmount").value(90.00))
                .andExpect(jsonPath("$.discountRate").value(0.10))
                .andExpect(jsonPath("$.remainingSeats").value(48)).andReturn();
        String code = JsonPath.read(result.getResponse().getContentAsString(), "$.code");
        em.flush();
        em.clear();
        TicketPurchase saved = em.createQuery("select p from TicketPurchase p where p.code=:code", TicketPurchase.class)
                .setParameter("code", code).getSingleResult();
        assertThat(saved.getCustomer().getId()).isEqualTo(customer);
        assertThat(saved.getConcert().getId()).isEqualTo(concert.getId());
        assertThat(saved.getConcert().getVenue().getCapacity()).isEqualTo(50);
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("90.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"none", "expired", "future"})
    void chargesFullPriceWithoutCurrentlyValidCard(String state) throws Exception {
        long customer = register();
        Concert concert = catalog(new BigDecimal("50.00"));
        LocalDate today = LocalDate.now(clock);
        if (!state.equals("none")) {
            LocalDate start = state.equals("expired") ? today.minusYears(1) : today.plusDays(1);
            LocalDate end = state.equals("expired") ? today : today.plusYears(1);
            em.persist(new LoyaltyCard(em.find(Customer.class, customer), start, end));
            em.flush();
        }
        purchase(customer, concert.getId(), 2).andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(100.00))
                .andExpect(jsonPath("$.discountRate").value(0));
    }

    @Test
    void renewsCardOnItsExclusiveExpiryDate() throws Exception {
        long customer = register();
        LocalDate today = LocalDate.now(clock);
        em.persist(new LoyaltyCard(em.find(Customer.class, customer), today.minusYears(1), today));
        em.flush();
        mvc.perform(post("/api/clients/{id}/carte-fidelite", customer).with(httpBasic("client", "ClientDemo123!")))
                .andExpect(status().isCreated());
    }

    @Test
    void returns404ForMissingCustomerOrConcertAnd400ForInvalidQuantity() throws Exception {
        long customer = register();
        Concert concert = catalog(new BigDecimal("50.00"));
        mvc.perform(post("/api/clients/{id}/carte-fidelite", Long.MAX_VALUE).with(httpBasic("admin", "AdminDemo123!")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CLIENT_INTROUVABLE"));
        purchase(Long.MAX_VALUE, concert.getId(), 1).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_INTROUVABLE"));
        purchase(customer, Long.MAX_VALUE, 1).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONCERT_INTROUVABLE"));
        purchase(customer, concert.getId(), 7).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ECHOUEE"));
    }

    @Test
    void fillsVenueExactlyAndRejectsFurtherPurchaseWithoutWriting() throws Exception {
        long customer = register();
        Customer buyer = em.find(Customer.class, customer);
        Concert concert = catalog(new BigDecimal("50.00"));
        for (int quantity : new int[]{6, 6, 6, 6, 6, 6, 6, 2}) {
            em.persist(new TicketPurchase(UUID.randomUUID().toString(), buyer, concert.getId(), quantity,
                    new BigDecimal("50.00").multiply(BigDecimal.valueOf(quantity)), LocalDateTime.now(clock)));
        }
        em.flush();
        purchase(customer, concert.getId(), 6).andExpect(status().isCreated())
                .andExpect(jsonPath("$.remainingSeats").value(0));
        long count = jdbc.queryForObject("SELECT COUNT(*) FROM ticket_purchases", Long.class);
        purchase(customer, concert.getId(), 1).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JAUGE_INSUFFISANTE"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ticket_purchases", Long.class)).isEqualTo(count);
        assertThat(jdbc.queryForObject("SELECT SUM(quantity) FROM ticket_purchases", Long.class)).isEqualTo(50);
    }

    @Test
    void storesSixTicketsAtMaximumCatalogPriceWithoutDecimalOverflow() throws Exception {
        long customer = register();
        Concert concert = catalog(new BigDecimal("99999999.99"));
        purchase(customer, concert.getId(), 6).andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(599999999.94));
    }

    @Test
    void deniesPurchasesWithoutClientRoleAndCardsWithoutClientOrAdminRole() throws Exception {
        mvc.perform(post("/api/achats").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/achats").with(httpBasic("producteur", "ProducteurDemo123!"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/clients/1/carte-fidelite").with(httpBasic("producteur", "ProducteurDemo123!")))
                .andExpect(status().isForbidden());
    }

    @Test
    void preservesThymeleafFormsWithCsrfProtection() throws Exception {
        mvc.perform(get("/clients/inscription")).andExpect(status().isOk())
                .andExpect(content().string(containsString("_csrf")));
        mvc.perform(get("/css/style.css")).andExpect(status().isOk());
        mvc.perform(post("/clients/inscription").param("name", "Alice").param("email", "alice@example.com")
                        .param("street", "Rue de Paris").param("city", "Nancy"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/clients/inscription").with(csrf()).param("name", "Alice")
                        .param("email", "alice@example.com").param("street", "Rue de Paris").param("city", "Nancy"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("message"));
        Long customer = jdbc.queryForObject("SELECT id FROM customers WHERE email='alice@example.com'", Long.class);
        mvc.perform(get("/fidelite").with(httpBasic("client", "ClientDemo123!"))).andExpect(status().isOk());
        mvc.perform(post("/fidelite").with(httpBasic("client", "ClientDemo123!")).param("customerId", customer.toString()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/fidelite").with(httpBasic("client", "ClientDemo123!")).with(csrf())
                        .param("customerId", customer.toString()))
                .andExpect(status().isOk()).andExpect(model().attributeExists("message"));
        Concert concert = catalog(new BigDecimal("50.00"));
        mvc.perform(post("/billets/acheter").with(httpBasic("client", "ClientDemo123!")).with(csrf())
                        .param("customerId", customer.toString()).param("concertId", concert.getId().toString()).param("quantity", "2"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("message"));
    }

    private long register() throws Exception {
        MvcResult result = mvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content(customerJson()))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    void renderedFormTokenAndCookieWorkTogetherWithoutDisablingCsrf() throws Exception {
        MvcResult page = mvc.perform(get("/clients/inscription")).andExpect(status().isOk()).andReturn();
        CsrfToken token = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
        var cookie = page.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        mvc.perform(post("/clients/inscription").cookie(cookie).param(token.getParameterName(), token.getToken())
                        .param("name", "Alice").param("email", "alice@example.com").param("street", "Rue").param("city", "Paris"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("message"));
        mvc.perform(post("/clients/inscription").cookie(cookie).param(token.getParameterName(), "incorrect")
                        .param("name", "Bob").param("email", "bob@example.com").param("street", "Rue").param("city", "Paris"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsPurchaseForConcertThatHasAlreadyStarted() throws Exception {
        long customer = register();
        Concert concert = catalog(new BigDecimal("50.00"));
        jdbc.update("UPDATE concerts SET date_time=? WHERE id=?", LocalDateTime.now(clock).minusDays(1), concert.getId());
        em.clear();
        purchase(customer, concert.getId(), 1).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCERT_TERMINE"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ticket_purchases", Integer.class)).isZero();
    }

    @Test
    void mysqlRejectsDuplicateCustomerEmail() throws Exception {
        register();
        assertThatThrownBy(() -> jdbc.update("INSERT INTO customers (name,email,street,city) VALUES (?,?,?,?)",
                "Other", "alice@example.com", "Rue", "Paris"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7})
    void mysqlRejectsInvalidPurchaseQuantity(int quantity) throws Exception {
        long customer = register();
        Concert concert = catalog(new BigDecimal("50.00"));
        assertThatThrownBy(() -> jdbc.update("INSERT INTO ticket_purchases (code,customer_id,concert_id,quantity,total_amount,created_at) VALUES (?,?,?,?,?,?)",
                UUID.randomUUID().toString(), customer, concert.getId(), quantity, new BigDecimal("50.00"), LocalDateTime.now(clock)))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("chk_purchase_quantity");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-1.00"})
    void mysqlRejectsNonPositivePurchaseTotal(String total) throws Exception {
        long customer = register();
        Concert concert = catalog(new BigDecimal("50.00"));
        assertThatThrownBy(() -> jdbc.update("INSERT INTO ticket_purchases (code,customer_id,concert_id,quantity,total_amount,created_at) VALUES (?,?,?,?,?,?)",
                UUID.randomUUID().toString(), customer, concert.getId(), 1, new BigDecimal(total), LocalDateTime.now(clock)))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("chk_purchase_total_positive");
    }

    @ParameterizedTest
    @ValueSource(strings = {"customer", "concert"})
    void mysqlRejectsMissingPurchaseReferences(String missing) throws Exception {
        long customer = register();
        Concert concert = catalog(new BigDecimal("50.00"));
        assertThatThrownBy(() -> jdbc.update("INSERT INTO ticket_purchases (code,customer_id,concert_id,quantity,total_amount,created_at) VALUES (?,?,?,?,?,?)",
                UUID.randomUUID().toString(), missing.equals("customer") ? Long.MAX_VALUE : customer,
                missing.equals("concert") ? Long.MAX_VALUE : concert.getId(), 1, new BigDecimal("50.00"), LocalDateTime.now(clock)))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("fk_purchase_" + missing);
    }

    @Test
    void mysqlRejectsCardWithNonIncreasingValidityPeriod() throws Exception {
        long customer = register();
        LocalDate today = LocalDate.now(clock);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO loyalty_cards (customer_id,purchased_at,expires_at) VALUES (?,?,?)",
                customer, today, today))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("chk_loyalty_period");
    }

    private String customerJson() {
        return "{\"name\":\"Alice\",\"email\":\"alice@example.com\",\"street\":\"Rue de Paris\",\"city\":\"Nancy\"}";
    }

    private Concert catalog(BigDecimal price) {
        Artist artist = new Artist("Nina Simone", "Jazz");
        Venue venue = new Venue("Olympia", "Paris", 50);
        em.persist(artist);
        em.persist(venue);
        Concert concert = new Concert("Jazz Night", artist, venue, LocalDateTime.now(clock).plusDays(30), price);
        em.persist(concert);
        em.flush();
        return concert;
    }

    private org.springframework.test.web.servlet.ResultActions purchase(long customer, long concert, int quantity)
            throws Exception {
        String json = "{\"concertId\":" + concert + ",\"customerId\":" + customer + ",\"quantity\":" + quantity + "}";
        return mvc.perform(post("/api/achats").with(httpBasic("client", "ClientDemo123!"))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
