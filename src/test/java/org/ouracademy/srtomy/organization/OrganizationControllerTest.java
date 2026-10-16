package org.ouracademy.srtomy.organization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class OrganizationControllerTest {

  @LocalServerPort int port;
  RestTestClient restTestClient;

  @BeforeEach
  void setUp() {
    restTestClient = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
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
