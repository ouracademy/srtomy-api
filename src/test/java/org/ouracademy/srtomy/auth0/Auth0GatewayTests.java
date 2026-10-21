package org.ouracademy.srtomy.auth0;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Uses a plain JDK HttpServer instead of mocking RestClient, so we assert on the literal HTTP
 * request path actually sent over the wire — the thing that matters for the path-traversal fix in
 * updateAppMetadata (building the URI with a template variable instead of string concatenation).
 */
class Auth0GatewayTests {

  private HttpServer server;
  private final BlockingQueue<String> receivedPaths = new ArrayBlockingQueue<>(1);

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          receivedPaths.offer(exchange.getRequestURI().getRawPath());
          byte[] body = "{}".getBytes();
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  private Auth0Gateway gatewayPointingAtTestServer() {
    var config = new Auth0Config();
    config.setUrl("http://127.0.0.1:" + server.getAddress().getPort());
    return new Auth0Gateway(config);
  }

  @Test
  void updateAppMetadataSendsTheUserIdAsASinglePathSegment() throws InterruptedException {
    var gateway = gatewayPointingAtTestServer();
    var accessToken = new AccessToken();
    accessToken.setAccessToken("fake-token");

    gateway.updateAppMetadata(accessToken, "auth0|abc123", Map.of("role", UUID.randomUUID()));

    String path = receivedPaths.poll(5, java.util.concurrent.TimeUnit.SECONDS);
    assertThat(path).isEqualTo("/api/v2/users/auth0%7Cabc123");
  }

  @Test
  void updateAppMetadataDoesNotLetAUserIdEscapeItsPathSegment() throws InterruptedException {
    var gateway = gatewayPointingAtTestServer();
    var accessToken = new AccessToken();
    accessToken.setAccessToken("fake-token");

    // A userId containing "/" must not be able to redirect the request to a
    // different Auth0 Management API endpoint.
    gateway.updateAppMetadata(accessToken, "../other-endpoint", Map.of("role", UUID.randomUUID()));

    String path = receivedPaths.poll(5, java.util.concurrent.TimeUnit.SECONDS);
    assertThat(path)
        .isEqualTo("/api/v2/users/..%2Fother-endpoint")
        .doesNotContain("/api/v2/other-endpoint");
  }
}
