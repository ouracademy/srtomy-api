package org.ouracademy.ovejafiel.shared;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Smoke level of the test pyramid (constitution, principle II): the application starts on a real
 * port and answers the basics. CI runs it last, after the unit and integration tests; anything more
 * than "it starts and answers" belongs in a lower level.
 */
// Without TestSecurityConfig, SecurityConfig's real JwtDecoder bean would call out to Auth0's
// live OIDC discovery endpoint on every context load — never acceptable in a test.
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class ApplicationSmokeTests {

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;

  private RestTestClient client() {
    return RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void theHealthProbeAnswersWithoutAToken() {
    client().get().uri("/actuator/health").exchange().expectStatus().isOk();
  }

  @Test
  void anEndpointRefusesARequestWithoutAToken() {
    client().get().uri("/v1/people").exchange().expectStatus().isUnauthorized();
  }

  @Test
  void theOpenApiDocumentIsServed() {
    client()
        .get()
        .uri("/v3/api-docs")
        // Outside the local profile the document is not public.
        .headers(headers -> headers.setBearerAuth(TestSecurityConfig.validToken(jwtEncoder)))
        .exchange()
        .expectStatus()
        .isOk();
  }
}
