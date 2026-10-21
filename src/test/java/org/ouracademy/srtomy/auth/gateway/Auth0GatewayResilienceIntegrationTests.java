package org.ouracademy.srtomy.auth.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.shared.SrtomyApplication;
import org.ouracademy.srtomy.shared.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpServerErrorException;

/**
 * Unlike Auth0GatewayTest (which builds a plain `new Auth0Gateway(...)`, bypassing Spring AOP),
 * this autowires the real Spring-managed bean so the @Retry/@CircuitBreaker annotations — which
 * only take effect through the proxy Spring creates — actually apply.
 *
 * <p>Imports TestSecurityConfig so SecurityConfig's real JwtDecoder bean (which would otherwise
 * call out to {@code auth0.url}'s OIDC discovery endpoint on startup) never runs — doubly important
 * here since auth0.url is pointed at the always-500 test server below, which would make that call
 * fail outright.
 */
@SpringBootTest(classes = SrtomyApplication.class)
@Import(TestSecurityConfig.class)
class Auth0GatewayResilienceIntegrationTests {

  private static final AtomicInteger requestCount = new AtomicInteger();
  private static HttpServer server;

  @DynamicPropertySource
  static void pointAuth0AtAFailingServer(DynamicPropertyRegistry registry) throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          requestCount.incrementAndGet();
          exchange.sendResponseHeaders(500, -1);
          exchange.close();
        });
    server.start();
    registry.add("auth0.url", () -> "http://127.0.0.1:" + server.getAddress().getPort());
  }

  @AfterAll
  static void stopServer() {
    server.stop(0);
  }

  @Autowired Auth0Gateway gateway;

  @Test
  void retriesOnServerErrorsBeforeGivingUp() {
    requestCount.set(0);

    assertThatThrownBy(gateway::getAccessToken).isInstanceOf(HttpServerErrorException.class);

    // max-attempts: 3 for the "auth0" retry instance (application.yml).
    assertThat(requestCount.get()).isEqualTo(3);
  }
}
