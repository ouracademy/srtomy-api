package org.ouracademy.srtomy.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class EventControllerTest {

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  RestTestClient restTestClient;

  @BeforeEach
  void setUp() {
    restTestClient =
        RestTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .defaultHeaders(
                headers -> headers.setBearerAuth(TestSecurityConfig.validToken(jwtEncoder)))
            .build();
  }

  @Test
  void findAllForAnUnknownOrganizationAndDateRangeReturnsAnEmptyList() {
    restTestClient
        .get()
        .uri("/events?organizationId=999999&start=2026-01-01T00:00:00Z&end=2026-12-31T00:00:00Z")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .value(body -> assertThat(body).isEqualTo("[]"));
  }

  @Test
  void deletingAMissingEventReturnsA404Problem() {
    // Exercises GlobalExceptionHandler end-to-end: singleEventRepository.findById(id)
    // .orElseThrow() in EventController.delete throws NoSuchElementException for an id
    // that doesn't exist.
    restTestClient
        .method(HttpMethod.DELETE)
        .uri("/events/999999")
        .contentType(MediaType.APPLICATION_JSON)
        .body("{}")
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectHeader()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON);
  }
}
