package com.example.concertticketingsystem;

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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"DEMO_PRODUCTEUR_PASSWORD=ProducteurDemo123!", "DEMO_ADMIN_PASSWORD=AdminDemo123!",
        "DEMO_CLIENT_PASSWORD=ClientDemo123!"})
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
class OpenApiAndHealthTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void servesActualSwaggerUiAndResourcesWithoutAuthentication() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/swagger-ui/index.html"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
        mvc.perform(get("/swagger-ui/swagger-ui-bundle.js")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.configUrl").value("/v3/api-docs/swagger-config"));
    }

    @Test
    void documentsActualCatalogPathsAndBasicAuthentication() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Concert Ticketing System — Catalogue"))
                .andExpect(jsonPath("$.paths['/api/concerts'].get").exists())
                .andExpect(jsonPath("$.paths['/api/concerts/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/production/artistes'].post").exists())
                .andExpect(jsonPath("$.paths['/api/production/concerts'].post").exists())
                .andExpect(jsonPath("$.paths['/api/admin/salles'].post").exists())
                .andExpect(jsonPath("$.components.securitySchemes.basicAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.basicAuth.scheme").value("basic"))
                .andExpect(jsonPath("$.paths['/api/concerts'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/production/artistes'].post.security[0].basicAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/production/concerts'].post.security[0].basicAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/admin/salles'].post.security[0].basicAuth").isArray());
    }

    @Test
    void documentsOptionalInclusiveDateAndArtistFilters() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/concerts'].get.parameters[*].name", containsInAnyOrder("du", "au", "artiste")))
                .andExpect(jsonPath("$.paths['/api/concerts'].get.parameters[*].required", contains(false, false, false)))
                .andExpect(jsonPath("$.paths['/api/concerts'].get.parameters[?(@.name=='du')].schema.format", contains("date")))
                .andExpect(jsonPath("$.paths['/api/concerts'].get.parameters[?(@.name=='au')].description", contains(containsString("inclusive"))))
                .andExpect(jsonPath("$.paths['/api/concerts'].get.parameters[?(@.name=='artiste')].schema.type", contains("integer")));
    }

    @Test
    void documentsDtoRequirementsBoundsAndNestedResponses() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.ArtistRequest.required", contains("name")))
                .andExpect(jsonPath("$.components.schemas.VenueRequest.required", containsInAnyOrder("name", "city", "capacity")))
                .andExpect(jsonPath("$.components.schemas.VenueRequest.properties.capacity.minimum").value(50))
                .andExpect(jsonPath("$.components.schemas.VenueRequest.properties.capacity.maximum").value(50000))
                .andExpect(jsonPath("$.components.schemas.ConcertRequest.required", hasItems("title", "artistId", "venueId", "dateTime", "unitPrice")))
                .andExpect(jsonPath("$.components.schemas.ConcertRequest.properties.dateTime.description", containsString("futures")))
                .andExpect(jsonPath("$.components.schemas.ConcertRequest.properties.dateTime.format").value("date-time"))
                .andExpect(jsonPath("$.components.schemas.ConcertRequest.properties.unitPrice.multipleOf").value(0.01))
                .andExpect(jsonPath("$.components.schemas.ConcertResponse.properties.artist['$ref']").value("#/components/schemas/ArtistResponse"))
                .andExpect(jsonPath("$.components.schemas.ConcertResponse.properties.venue['$ref']").value("#/components/schemas/VenueResponse"))
                .andExpect(jsonPath("$.components.schemas.ApiError.required", hasItems("timestamp", "status", "code", "message", "path")));
    }

    @Test
    void documentsSuccessValidationMissingReferenceAndConflictResponses() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/concerts'].get.responses['200'].content['application/json'].schema.type").value("array"))
                .andExpect(jsonPath("$.paths['/api/concerts'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/concerts'].get.responses['400'].content['application/json'].schema['$ref']").value("#/components/schemas/ApiError"))
                .andExpect(jsonPath("$.paths['/api/concerts/{id}'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/production/concerts'].post.responses['201'].headers.Location").exists())
                .andExpect(jsonPath("$.paths['/api/production/concerts'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/production/concerts'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/production/concerts'].post.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/production/concerts'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/production/concerts'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/production/artistes'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/production/artistes'].post.requestBody.content['application/json'].schema['$ref']").value("#/components/schemas/ArtistRequest"))
                .andExpect(jsonPath("$.paths['/api/production/artistes'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/admin/salles'].post.responses['409']").exists());
    }

    @Test
    void includesExposedActuatorHealthInOpenApi() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/actuator/health'].get").exists())
                .andExpect(jsonPath("$.paths['/actuator/health'].get.responses['200']").exists());
    }

    @Test
    void servesOpenApiYamlWithoutAuthentication() throws Exception {
        mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk())
                .andExpect(content().string(containsString("openapi:")))
                .andExpect(content().string(containsString("/api/production/artistes:")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"})
    void exposesActualHealthyActuatorWithoutInternalDetails(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"producteur", "admin"})
    void createsAndReadsCatalogUsingActualBasicSecurityAndMySql(String producer) throws Exception {
        String password = producer.equals("admin") ? "AdminDemo123!" : "ProducteurDemo123!";
        long artist = createdId("/api/production/artistes", producer, password,
                "{\"name\":\"Nina Simone\",\"musicalGenre\":\"Jazz\"}");
        long venue = createdId("/api/admin/salles", "admin", "AdminDemo123!",
                "{\"name\":\"Olympia\",\"city\":\"Paris\",\"capacity\":500}");
        String request = "{\"title\":\"Jazz Night\",\"artistId\":" + artist + ",\"venueId\":" + venue
                + ",\"dateTime\":\"" + LocalDateTime.now().plusDays(30) + "\",\"unitPrice\":42.50}";
        long concert = createdId("/api/production/concerts", producer, password, request);
        mvc.perform(get("/api/concerts/{id}", concert)).andExpect(status().isOk())
                .andExpect(jsonPath("$.artist.id").value(artist))
                .andExpect(jsonPath("$.venue.id").value(venue));
        mvc.perform(post("/api/production/artistes").with(httpBasic(producer, password))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Nina Simone\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void verifies401403400And404ThroughActualSecurityAndControllers() throws Exception {
        mvc.perform(post("/api/admin/salles").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/salles").with(httpBasic("client", "ClientDemo123!"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/salles").with(httpBasic("admin", "AdminDemo123!"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/concerts/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    private long createdId(String path, String username, String password, String json) throws Exception {
        MvcResult result = mvc.perform(post(path).with(httpBasic(username, password))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }
}
