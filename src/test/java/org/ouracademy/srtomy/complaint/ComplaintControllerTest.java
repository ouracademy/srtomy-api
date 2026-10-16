package org.ouracademy.srtomy.complaint;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class ComplaintControllerTest {

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
  void createWithAllRequiredFieldsReturns201() {
    var complaint = new Complaint();
    complaint.setFirstName("Juan");
    complaint.setLastName("Perez");
    // dni has no @NotBlank/@NotNull on the entity (only @Pattern, which null
    // satisfies), but the DB column is NOT NULL (see V1.0.0 migration) — a
    // real request without it would 500 instead of 400. Out of scope for
    // this test to fix; supplying a valid value here either way.
    complaint.setDni("12345678");
    complaint.setEmail("juan@example.com");
    complaint.setComment("Everything was great");

    var created =
        restTestClient
            .post()
            .uri("/complaints")
            .contentType(MediaType.APPLICATION_JSON)
            .body(complaint)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(Complaint.class)
            .returnResult()
            .getResponseBody();

    assertThat(created.getId()).isNotNull();
  }

  @Test
  void createWithoutRequiredFieldsIsABadRequest() {
    restTestClient
        .post()
        .uri("/complaints")
        .contentType(MediaType.APPLICATION_JSON)
        .body(new Complaint())
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
