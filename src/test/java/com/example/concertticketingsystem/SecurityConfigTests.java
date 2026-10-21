package com.example.concertticketingsystem;

import java.util.Map;
import java.util.stream.Stream;

import com.example.concertticketingsystem.config.ClockConfig;
import com.example.concertticketingsystem.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTests.RouteProbeController.class, properties = {
        "DEMO_CLIENT_PASSWORD=ClientDemo123!",
        "DEMO_PRODUCTEUR_PASSWORD=ProducteurDemo123!",
        "DEMO_ADMIN_PASSWORD=AdminDemo123!"})
@Import({SecurityConfig.class, ClockConfig.class, SecurityConfigTests.RouteProbeController.class})
class SecurityConfigTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private UserDetailsService users;

    @ParameterizedTest
    @MethodSource("publicRoutes")
    void publicRoutesDoNotRequireAuthentication(String method, String path) throws Exception {
        mvc.perform(request(method, path)).andExpect(status().is2xxSuccessful())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    static Stream<Arguments> publicRoutes() {
        return Stream.of(
                Arguments.of("GET", "/api/concerts"),
                Arguments.of("GET", "/api/concerts/1"),
                Arguments.of("POST", "/api/clients"),
                Arguments.of("GET", "/actuator/health"),
                Arguments.of("GET", "/actuator/health/readiness"),
                Arguments.of("GET", "/v3/api-docs"),
                Arguments.of("GET", "/v3/api-docs/swagger-config"),
                Arguments.of("GET", "/swagger-ui.html"),
                Arguments.of("GET", "/swagger-ui/index.html"));
    }

    @ParameterizedTest
    @MethodSource("protectedRoutes")
    void missingCredentialsReturn401WithBasicChallenge(String path) throws Exception {
        mvc.perform(post(path))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"Concert Ticketing System\""))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTHENTIFICATION_REQUISE"))
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    static Stream<String> protectedRoutes() {
        return Stream.of("/api/achats", "/api/clients/1/carte-fidelite",
                "/api/production/artistes", "/api/production/concerts", "/api/admin/salles");
    }

    @ParameterizedTest
    @MethodSource("allowedWrites")
    void correctBasicCredentialsAllowWriteWithoutCsrfToken(String username, String password, String path)
            throws Exception {
        mvc.perform(post(path).with(httpBasic(username, password)))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    static Stream<Arguments> allowedWrites() {
        return Stream.of(
                Arguments.of("client", "ClientDemo123!", "/api/achats"),
                Arguments.of("client", "ClientDemo123!", "/api/clients/1/carte-fidelite"),
                Arguments.of("producteur", "ProducteurDemo123!", "/api/production/artistes"),
                Arguments.of("producteur", "ProducteurDemo123!", "/api/production/concerts"),
                Arguments.of("admin", "AdminDemo123!", "/api/production/artistes"),
                Arguments.of("admin", "AdminDemo123!", "/api/production/concerts"),
                Arguments.of("admin", "AdminDemo123!", "/api/admin/salles"),
                Arguments.of("admin", "AdminDemo123!", "/api/clients/1/carte-fidelite"));
    }

    @ParameterizedTest
    @MethodSource("forbiddenWrites")
    void authenticatedUserWithoutRoleGets403(String username, String password, String path) throws Exception {
        mvc.perform(post(path).with(httpBasic(username, password)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCES_INTERDIT"))
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    static Stream<Arguments> forbiddenWrites() {
        return Stream.of(
                Arguments.of("client", "ClientDemo123!", "/api/production/artistes"),
                Arguments.of("client", "ClientDemo123!", "/api/production/concerts"),
                Arguments.of("client", "ClientDemo123!", "/api/admin/salles"),
                Arguments.of("producteur", "ProducteurDemo123!", "/api/achats"),
                Arguments.of("producteur", "ProducteurDemo123!", "/api/clients/1/carte-fidelite"),
                Arguments.of("producteur", "ProducteurDemo123!", "/api/admin/salles"),
                Arguments.of("admin", "AdminDemo123!", "/api/achats"));
    }

    @Test
    void adminCanAccessOtherAdministrationOperations() throws Exception {
        mvc.perform(get("/api/admin/security-probe").with(httpBasic("admin", "AdminDemo123!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/security-probe").with(httpBasic("client", "ClientDemo123!")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @MethodSource("invalidCredentials")
    void incorrectCredentialsAlwaysReturn401(String username, String password) throws Exception {
        mvc.perform(post("/api/admin/salles").with(httpBasic(username, password)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Basic ")))
                .andExpect(jsonPath("$.code").value("AUTHENTIFICATION_REQUISE"));
    }

    static Stream<Arguments> invalidCredentials() {
        return Stream.of(Arguments.of("admin", "incorrect"), Arguments.of("unknown", "incorrect"));
    }

    @Test
    void authenticationIsNotRememberedBetweenRequests() throws Exception {
        MvcResult authenticated = mvc.perform(get("/api/admin/security-probe")
                        .with(httpBasic("admin", "AdminDemo123!")))
                .andExpect(status().isOk()).andExpect(header().doesNotExist("Set-Cookie")).andReturn();
        assertThat(authenticated.getRequest().getSession(false)).isNull();
        mvc.perform(get("/api/admin/security-probe")).andExpect(status().isUnauthorized());
    }

    @Test
    void ignoresAuthenticationStoredInAnIncomingSession() throws Exception {
        UserDetails admin = users.loadUserByUsername("admin");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(
                        admin, null, admin.getAuthorities())));
        mvc.perform(get("/api/admin/security-probe").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void deniesUnlistedRoutesAndDoesNotExposeHtmlLogin() throws Exception {
        mvc.perform(get("/api/unlisted").with(httpBasic("admin", "AdminDemo123!")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/login")).andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
        mvc.perform(post("/api/concerts").with(httpBasic("client", "ClientDemo123!")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @MethodSource("demoAccounts")
    void storesOnlyBcryptEncodedPasswords(String username, String password, String role) {
        UserDetails user = users.loadUserByUsername(username);
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(user.getPassword()).startsWith("$2").isNotEqualTo(password);
        assertThat(encoder.matches(password, user.getPassword())).isTrue();
        assertThat(encoder.matches("incorrect", user.getPassword())).isFalse();
        assertThat(user.getAuthorities()).extracting("authority").containsExactly("ROLE_" + role);
    }

    static Stream<Arguments> demoAccounts() {
        return Stream.of(Arguments.of("client", "ClientDemo123!", "CLIENT"),
                Arguments.of("producteur", "ProducteurDemo123!", "PRODUCTEUR"),
                Arguments.of("admin", "AdminDemo123!", "ADMIN"));
    }

    private MockHttpServletRequestBuilder request(String method, String path) {
        return method.equals("GET") ? get(path) : post(path);
    }

    // Sondes de test : aucun controller metier de la Personne B n'est cree ou modifie.
    @RestController
    static class RouteProbeController {
        @GetMapping({"/api/concerts", "/api/concerts/{id}", "/actuator/health", "/actuator/health/readiness",
                "/v3/api-docs", "/v3/api-docs/swagger-config", "/swagger-ui.html", "/swagger-ui/index.html",
                "/api/admin/security-probe"})
        Map<String, Boolean> read() {
            return Map.of("ok", true);
        }

        @PostMapping({"/api/clients", "/api/achats", "/api/clients/{id}/carte-fidelite",
                "/api/production/artistes", "/api/production/concerts", "/api/admin/salles"})
        @ResponseStatus(HttpStatus.CREATED)
        Map<String, Boolean> write() {
            return Map.of("ok", true);
        }
    }
}
