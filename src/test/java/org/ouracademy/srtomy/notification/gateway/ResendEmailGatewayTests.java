package org.ouracademy.srtomy.notification.gateway;

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
  AtomicReference<String> idempotencyKey = new AtomicReference<>();
  volatile int status = 200;

  @BeforeEach
  void startServer() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/emails",
        exchange -> {
          body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          idempotencyKey.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
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
    return gateway("");
  }

  ResendEmailGateway gateway(String replyTo) {
    return new ResendEmailGateway(
        "key-123",
        "Sr. Tommy <no-reply@x.org>",
        "http://127.0.0.1:" + server.getAddress().getPort(),
        replyTo);
  }

  @Test
  void postsTheMessageWithTheApiKey() {
    gateway().send("msg-1", "ana@x.org", "Invitación", "Entra aquí");

    assertThat(authorization.get()).isEqualTo("Bearer key-123");
    // A retry of the same message cannot deliver twice.
    assertThat(idempotencyKey.get()).isEqualTo("msg-1");
    assertThat(body.get())
        .contains("\"to\":[\"ana@x.org\"]")
        .contains("\"subject\":\"Invitación\"")
        .contains("\"text\":\"Entra aquí\"")
        .contains("\"from\":\"Sr. Tommy <no-reply@x.org>\"");
  }

  @Test
  void repliesGoToTheSupportAddressWhenThereIsOne() {
    gateway("soporte@x.org").send("msg-1", "ana@x.org", "S", "T");

    assertThat(body.get()).contains("\"reply_to\":\"soporte@x.org\"");
  }

  @Test
  void withoutASupportAddressThereIsNoReplyTo() {
    gateway().send("msg-1", "ana@x.org", "S", "T");

    assertThat(body.get()).doesNotContain("reply_to");
  }

  @Test
  void aRejectedMessageThrows() {
    status = 422;

    assertThatThrownBy(() -> gateway().send("msg-1", "ana@x.org", "S", "T"))
        .isInstanceOf(RuntimeException.class);
  }
}
