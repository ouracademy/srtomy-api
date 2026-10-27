package org.ouracademy.ovejafiel.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

/** The daily job is an internal endpoint that only Cloud Scheduler's account may call. */
@SpringBootTest(classes = OvejafielApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class EventJobsIntegrationTests {

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  RestTestClient client;

  @BeforeEach
  void setUp() {
    client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  RestTestClient.ResponseSpec extendSeries(String token) {
    return client
        .post()
        .uri("/v1/internal/events/extend-series")
        .headers(
            headers -> {
              if (token != null) {
                headers.setBearerAuth(token);
              }
            })
        .exchange();
  }

  @Test
  void theSchedulerExtendsTheSeries() {
    var body =
        extendSeries(TestSecurityConfig.schedulerToken(jwtEncoder))
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(body).contains("createdEvents");
  }

  @Test
  void withoutATokenItIs401() {
    extendSeries(null).expectStatus().isUnauthorized();
  }

  @Test
  void anAuth0TokenOfAUserIs401() {
    extendSeries(TestSecurityConfig.validToken(jwtEncoder)).expectStatus().isUnauthorized();
  }
}
