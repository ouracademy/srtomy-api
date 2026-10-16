package org.ouracademy.srtomy.organization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class OrganizationControllerTest {

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
  void predefinedRolesReturnsTheSeededRoles() {
    restTestClient
        .get()
        .uri("/organizations/locations/predefined-roles")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .value(
            body ->
                org.assertj.core.api.Assertions.assertThat(body)
                    .contains("PASTOR", "WORSHIP_LEADER", "USHER"));
  }
}
