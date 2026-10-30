package org.ouracademy.ovejafiel.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Requests run on virtual threads ({@code spring.threads.virtual.enabled}). */
@SpringBootTest(classes = OvejafielApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({TestSecurityConfig.class, VirtualThreadsIntegrationTests.ThreadProbe.class})
class VirtualThreadsIntegrationTests {

  /** Answers whether the thread that handles the request is a virtual one. */
  @RestController
  static class ThreadProbe {
    @GetMapping("/probe/thread")
    boolean virtual() {
      return Thread.currentThread().isVirtual();
    }
  }

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;

  @Test
  void aRequestIsHandledOnAVirtualThread() {
    var client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port + "/v1").build();

    var virtual =
        client
            .get()
            .uri("/probe/thread")
            .headers(h -> h.setBearerAuth(TestSecurityConfig.validToken(jwtEncoder)))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(virtual).isEqualTo("true");
  }
}
