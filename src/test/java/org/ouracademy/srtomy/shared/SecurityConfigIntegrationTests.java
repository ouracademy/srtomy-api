package org.ouracademy.srtomy.shared;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class SecurityConfigIntegrationTests {

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  RestTestClient restTestClient;

  @BeforeEach
  void setUp() {
    restTestClient = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void healthProbesArePublic() {
    restTestClient.get().uri("/actuator/health/liveness").exchange().expectStatus().isOk();
  }

  @Test
  void aProtectedEndpointWithoutATokenIs401() {
    restTestClient
        .get()
        .uri("/v1/organizations/locations/predefined-roles")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void aProtectedEndpointWithAValidTokenIsAllowed() {
    var token = TestSecurityConfig.validToken(jwtEncoder);

    restTestClient
        .get()
        .uri("/v1/organizations/locations/predefined-roles")
        .headers(headers -> headers.setBearerAuth(token))
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void aTokenForADifferentApiIs401() {
    var token = TestSecurityConfig.tokenWithWrongAudience(jwtEncoder);

    restTestClient
        .get()
        .uri("/v1/organizations/locations/predefined-roles")
        .headers(headers -> headers.setBearerAuth(token))
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }
}
