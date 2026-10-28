package org.ouracademy.ovejafiel.outbox.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.outbox.domain.OutboxChannel;
import org.ouracademy.ovejafiel.outbox.domain.OutboxMessageId;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

class CloudTasksGatewayTests {

  static final String TASKS_API = "https://tasks.example.org";
  static final String PROJECT = "my-project";
  static final String LOCATION = "my-region";
  static final String SCHEDULER = "scheduler@my-project.iam.gserviceaccount.com";
  static final String AUDIENCE = "https://api.example.org/internal";

  RestClient.Builder builder = RestClient.builder();
  MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
  OutboxMessageId id = OutboxMessageId.generate();

  // The target has a trailing slash on purpose: the gateway must not double it.
  CloudTasksGateway gateway =
      new CloudTasksGateway(
          builder,
          TASKS_API,
          () -> "token-1",
          PROJECT,
          LOCATION,
          "https://api.example.org/",
          SCHEDULER,
          AUDIENCE,
          Duration.ofSeconds(1));

  String tasksOf(String queue) {
    return TASKS_API
        + "/v2/projects/"
        + PROJECT
        + "/locations/"
        + LOCATION
        + "/queues/"
        + queue
        + "/tasks";
  }

  @Test
  void createsATaskInTheQueueOfTheChannelThatCallsBackTheApi() {
    server
        .expect(requestTo(tasksOf("outbox-email")))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer token-1"))
        .andExpect(
            jsonPath("$.task.name")
                .value(
                    "projects/%s/locations/%s/queues/outbox-email/tasks/%s"
                        .formatted(PROJECT, LOCATION, id.value())))
        .andExpect(jsonPath("$.task.httpRequest.httpMethod").value("POST"))
        .andExpect(
            jsonPath("$.task.httpRequest.url")
                .value("https://api.example.org/v1/internal/outbox/" + id.value() + "/attempt"))
        .andExpect(jsonPath("$.task.httpRequest.oidcToken.serviceAccountEmail").value(SCHEDULER))
        .andExpect(jsonPath("$.task.httpRequest.oidcToken.audience").value(AUDIENCE))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    gateway.schedule(id, OutboxChannel.EMAIL);

    server.verify();
  }

  @Test
  void theTaskIsScheduledAMomentAheadSoTheCommitIsUsuallyDoneWhenItArrives() {
    var before = Instant.now();
    server
        .expect(requestTo(tasksOf("outbox-email")))
        .andExpect(
            request -> {
              var body = ((MockClientHttpRequest) request).getBodyAsString();
              var scheduled = Instant.parse(JsonPath.read(body, "$.task.scheduleTime"));
              assertThat(scheduled).isBetween(before.plusMillis(900), Instant.now().plusSeconds(2));
            })
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    gateway.schedule(id, OutboxChannel.EMAIL);

    server.verify();
  }

  @Test
  void eachChannelHasItsOwnQueue() {
    server
        .expect(requestTo(tasksOf("outbox-whatsapp")))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    gateway.schedule(id, OutboxChannel.WHATSAPP);

    server.verify();
  }

  @Test
  void aTaskThatAlreadyExistsIsNotAnError() {
    server.expect(requestTo(tasksOf("outbox-email"))).andRespond(withStatus(HttpStatus.CONFLICT));

    assertThatCode(() -> gateway.schedule(id, OutboxChannel.EMAIL)).doesNotThrowAnyException();

    // The task was asked for once and the conflict was the answer.
    server.verify();
  }

  @Test
  void anyOtherRejectionIsReportedSoTheCallerRollsBack() {
    server.expect(requestTo(tasksOf("outbox-email"))).andRespond(withStatus(HttpStatus.FORBIDDEN));

    assertThatThrownBy(() -> gateway.schedule(id, OutboxChannel.EMAIL))
        .isInstanceOf(HttpClientErrorException.Forbidden.class);
  }

  @Test
  void aServerFailureIsReportedToo() {
    server
        .expect(requestTo(tasksOf("outbox-email")))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    assertThatThrownBy(() -> gateway.schedule(id, OutboxChannel.EMAIL))
        .isInstanceOf(HttpServerErrorException.ServiceUnavailable.class);
  }
}
