package com.example.concertticketingsystem.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI catalogOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Concert Ticketing System — Catalogue")
                        .version("1.0.0")
                        .description("Catalogue des artistes, salles et concerts. Les lectures sont publiques. "
                                + "Les créations utilisent HTTP Basic avec les rôles PRODUCTEUR et ADMIN. "
                                + "Les dates sont locales au format ISO ; du et au sont inclusifs."))
                .components(new Components().addSecuritySchemes("basicAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("basic")
                        .description("Compte de démonstration documenté dans le README. "
                                + "Utiliser HTTPS hors du poste local.")));
    }
}
