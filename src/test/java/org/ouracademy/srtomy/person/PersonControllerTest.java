package org.ouracademy.srtomy.person;

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
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class PersonControllerTest {

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
