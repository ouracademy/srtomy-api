package org.ouracademy.ovejafiel.notification.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResendEmailGatewayTests {

  HttpServer server;
  AtomicReference<String> body = new AtomicReference<>();
  AtomicReference<String> authorization = new AtomicReference<>();
  volatile int status = 200;

  @BeforeEach
  void startServer() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/emails",
        exchange -> {
          body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          var response = "{\"id\":\"1\"}".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  ResendEmailGateway gateway() {
    return new ResendEmailGateway(
        "key-123",
        "Sr. Tommy <no-reply@x.org>",
        "http://127.0.0.1:" + server.getAddress().getPort());
  }

  @Test
  void postsTheMessageWithTheApiKey() {
    gateway().send("ana@x.org", "Invitación", "Entra aquí");

    assertThat(authorization.get()).isEqualTo("Bearer key-123");
    assertThat(body.get())
        .contains("\"to\":[\"ana@x.org\"]")
        .contains("\"subject\":\"Invitación\"")
        .contains("\"text\":\"Entra aquí\"")
        .contains("\"from\":\"Sr. Tommy <no-reply@x.org>\"");
  }

  @Test
  void aRejectedMessageThrows() {
    status = 422;

    assertThatThrownBy(() -> gateway().send("ana@x.org", "S", "T"))
        .isInstanceOf(RuntimeException.class);
  }
}
