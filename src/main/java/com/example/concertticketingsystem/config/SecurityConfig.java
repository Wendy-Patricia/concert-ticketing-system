package com.example.concertticketingsystem.config;

import java.io.IOException;
import java.time.Clock;

import com.example.concertticketingsystem.dto.ApiError;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder,
            @Value("${DEMO_CLIENT_PASSWORD:ClientDemo123!}") String clientPassword,
            @Value("${DEMO_PRODUCTEUR_PASSWORD:ProducteurDemo123!}") String producerPassword,
            @Value("${DEMO_ADMIN_PASSWORD:AdminDemo123!}") String adminPassword) {
        return new InMemoryUserDetailsManager(
                User.withUsername("client").password(encoder.encode(clientPassword)).roles("CLIENT").build(),
                User.withUsername("producteur").password(encoder.encode(producerPassword)).roles("PRODUCTEUR").build(),
                User.withUsername("admin").password(encoder.encode(adminPassword)).roles("ADMIN").build());
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper, Clock clock)
            throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, exception) -> {
            response.setHeader("WWW-Authenticate", "Basic realm=\"Concert Ticketing System\"");
            writeError(request, response, mapper, clock, 401, "AUTHENTIFICATION_REQUISE",
                    "Authentification requise ou identifiants invalides.");
        };

        return http
                // REST : Authorization explicite ; formulaires Thymeleaf : jeton CSRF en cookie.
                .csrf(csrf -> csrf.csrfTokenRepository(new CookieCsrfTokenRepository())
                        .ignoringRequestMatchers("/api/**"))
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .httpBasic(basic -> basic.authenticationEntryPoint(unauthorized))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler((request, response, exception) -> writeError(
                                request, response, mapper, clock, 403, "ACCES_INTERDIT", "Accès interdit.")))
                .authorizeHttpRequests(requests -> requests
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/concerts", "/api/concerts/{id}",
                                "/actuator/health", "/actuator/health/**", "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml",
                                "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/clients").permitAll()
                        .requestMatchers(HttpMethod.GET, "/clients/inscription", "/css/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/clients/inscription").permitAll()
                        .requestMatchers("/fidelite").hasAnyRole("CLIENT", "ADMIN")
                        .requestMatchers("/billets/acheter").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.POST, "/api/achats").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.POST, "/api/clients/{id}/carte-fidelite")
                                .hasAnyRole("CLIENT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/production/artistes", "/api/production/concerts")
                                .hasAnyRole("PRODUCTEUR", "ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .build();
    }

    private static void writeError(HttpServletRequest request, HttpServletResponse response,
            ObjectMapper mapper, Clock clock, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(),
                new ApiError(clock.instant(), status, code, message, request.getRequestURI()));
    }
}
