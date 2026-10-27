package org.ouracademy.srtomy.outbox.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.outbox.domain.OutboxChannel;
import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;

class CloudTasksGatewayTests {

  HttpServer server;
  AtomicReference<String> path = new AtomicReference<>();
  AtomicReference<String> body = new AtomicReference<>();
  AtomicReference<String> authorization = new AtomicReference<>();
  volatile int status = 200;
  OutboxMessageId id = OutboxMessageId.generate();

  @BeforeEach
  void startServer() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          path.set(exchange.getRequestURI().getPath());
          body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          var response = "{}".getBytes(StandardCharsets.UTF_8);
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

  CloudTasksGateway gateway() {
    return new CloudTasksGateway(
        "http://127.0.0.1:" + server.getAddress().getPort(),
        () -> "token-1",
        "sr-tommy-dev",
        "southamerica-west1",
        "https://api.example.run.app/",
        "api-scheduler@sr-tommy-dev.iam.gserviceaccount.com",
        "https://api.srtommy.com/internal");
  }

  @Test
  void createsATaskInTheQueueOfTheChannelThatCallsBackTheApi() {
    gateway().schedule(id, OutboxChannel.EMAIL);

    assertThat(path.get())
        .isEqualTo(
            "/v2/projects/sr-tommy-dev/locations/southamerica-west1/queues/outbox-email/tasks");
    assertThat(authorization.get()).isEqualTo("Bearer token-1");
    assertThat(body.get())
        .contains(
            "\"name\":\"projects/sr-tommy-dev/locations/southamerica-west1/queues/outbox-email/tasks/"
                + id.value()
                + "\"")
        .contains("\"httpMethod\":\"POST\"")
        .contains(
            "\"url\":\"https://api.example.run.app/v1/internal/outbox/" + id.value() + "/attempt\"")
        .contains("\"serviceAccountEmail\":\"api-scheduler@sr-tommy-dev.iam.gserviceaccount.com\"")
        .contains("\"audience\":\"https://api.srtommy.com/internal\"");
  }

  @Test
  void eachChannelHasItsOwnQueue() {
    gateway().schedule(id, OutboxChannel.WHATSAPP);

    assertThat(path.get()).contains("/queues/outbox-whatsapp/tasks");
  }

  @Test
  void aTaskThatAlreadyExistsIsNotAnError() {
    status = 409;

    gateway().schedule(id, OutboxChannel.EMAIL);
  }

  @Test
  void anyOtherFailureIsReportedSoTheCallerRollsBack() {
    status = 403;

    assertThatThrownBy(() -> gateway().schedule(id, OutboxChannel.EMAIL))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void aServerFailureIsReportedToo() {
    status = 503;

    assertThatThrownBy(() -> gateway().schedule(id, OutboxChannel.EMAIL))
        .isInstanceOf(RuntimeException.class);
  }
}
