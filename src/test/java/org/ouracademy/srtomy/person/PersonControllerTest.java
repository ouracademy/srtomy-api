package org.ouracademy.srtomy.person;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class PersonControllerTest {

  @LocalServerPort int port;
  RestTestClient restTestClient;

  @BeforeEach
  void setUp() {
    restTestClient = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void findAllForAnUnknownOrganizationReturnsAnEmptyPage() {
    restTestClient
        .get()
        .uri("/people?organizationId=999999")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .value(body -> assertThat(body).contains("\"totalElements\":0"));
  }

  @Test
  void findAllWithoutOrganizationIdIsABadRequest() {
    restTestClient.get().uri("/people").exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
