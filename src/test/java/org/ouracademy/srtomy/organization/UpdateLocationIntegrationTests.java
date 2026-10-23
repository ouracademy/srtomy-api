package org.ouracademy.srtomy.organization;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.repository.OrganizationRepository;
import org.ouracademy.srtomy.shared.SrtomyApplication;
import org.ouracademy.srtomy.shared.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

/** Edit-location flow end to end: HTTP → handler → Postgres, read back through the API. */
@SpringBootTest(classes = SrtomyApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class UpdateLocationIntegrationTests {

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired OrganizationRepository organizationRepository;

  RestTestClient restTestClient;
  String bearer;

  @BeforeEach
  void setUp() {
    restTestClient = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    bearer = "Bearer " + TestSecurityConfig.validToken(jwtEncoder);
  }

  @Test
  void editingALocationUpdatesItsInfoAndItsCustomRoles() {
    var location =
        organizationRepository.save(new Organization("Sede Antigua", Organization.Type.LOCATION));
    var uri = "/v1/organizations/locations/" + location.getId().value();

    restTestClient
        .put()
        .uri(uri)
        .header("Authorization", bearer)
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            Map.of(
                "name", "Sede Norte",
                "description", "Principal",
                "address", "Av. Uno 123",
                "phoneNumber", "987654321",
                "roles", List.of("VOLUNTARIO")))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.name")
        .isEqualTo("Sede Norte");

    restTestClient
        .get()
        .uri(uri + "/roles")
        .header("Authorization", bearer)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$[?(@.name == 'VOLUNTARIO')]")
        .isNotEmpty();

    restTestClient
        .put()
        .uri(uri)
        .header("Authorization", bearer)
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("name", "Sede Norte", "roles", List.of()))
        .exchange()
        .expectStatus()
        .isOk();

    restTestClient
        .get()
        .uri(uri + "/roles")
        .header("Authorization", bearer)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$[?(@.name == 'VOLUNTARIO')]")
        .isEmpty();
  }

  @Test
  void editingAnUnknownLocationIsNotFound() {
    restTestClient
        .put()
        .uri("/v1/organizations/locations/" + UUID.randomUUID())
        .header("Authorization", bearer)
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("name", "Sede Norte", "roles", List.of()))
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void editingWithoutANameIsBadRequest() {
    var location =
        organizationRepository.save(new Organization("Sede Antigua", Organization.Type.LOCATION));

    restTestClient
        .put()
        .uri("/v1/organizations/locations/" + location.getId().value())
        .header("Authorization", bearer)
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("name", "", "roles", List.of()))
        .exchange()
        .expectStatus()
        .isBadRequest();
  }
}
