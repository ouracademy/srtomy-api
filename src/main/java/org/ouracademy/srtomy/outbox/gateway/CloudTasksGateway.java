package org.ouracademy.srtomy.outbox.gateway;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.ouracademy.srtomy.outbox.domain.OutboxChannel;
import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Creates a Cloud Tasks task that calls back the API with an ID token of the scheduler's service
 * account. One queue per channel ({@code outbox-email}, {@code outbox-whatsapp}), so each has its
 * own retry policy and rate limit. The task is named after the message, so creating it twice is
 * harmless.
 */
@Slf4j
class CloudTasksGateway implements TaskGateway {

  private final RestClient restClient;
  private final String project;
  private final String location;
  private final String targetUrl;
  private final String serviceAccountEmail;
  private final String audience;
  private final Duration delay;

  CloudTasksGateway(
      String apiUrl,
      Supplier<String> accessToken,
      String project,
      String location,
      String targetUrl,
      String serviceAccountEmail,
      String audience,
      Duration delay) {
    this.project = project;
    this.location = location;
    this.targetUrl = targetUrl.replaceAll("/+$", "");
    this.serviceAccountEmail = serviceAccountEmail;
    this.audience = audience;
    this.delay = delay;
    this.restClient =
        RestClient.builder()
            .baseUrl(apiUrl)
            .requestInterceptor(
                (request, body, execution) -> {
                  request.getHeaders().setBearerAuth(accessToken.get());
                  return execution.execute(request, body);
                })
            .build();
  }

  static String queueOf(OutboxChannel channel) {
    return "outbox-" + channel.name().toLowerCase();
  }

  @Override
  public void schedule(OutboxMessageId id, OutboxChannel channel) {
    var queuePath =
        "/v2/projects/%s/locations/%s/queues/%s".formatted(project, location, queueOf(channel));
    var taskName =
        "projects/%s/locations/%s/queues/%s/tasks/%s"
            .formatted(project, location, queueOf(channel), id.value());
    var task =
        Map.of(
            "name",
            taskName,
            // A few seconds ahead, so the transaction that enqueues the message has committed
            // when the task arrives.
            "scheduleTime",
            Instant.now().plus(delay).toString(),
            "httpRequest",
            Map.of(
                "httpMethod",
                "POST",
                "url",
                targetUrl + "/v1/internal/outbox/" + id.value() + "/attempt",
                "oidcToken",
                Map.of("serviceAccountEmail", serviceAccountEmail, "audience", audience)));
    try {
      restClient
          .post()
          .uri(queuePath + "/tasks")
          .contentType(MediaType.APPLICATION_JSON)
          .body(Map.of("task", task))
          .retrieve()
          .toBodilessEntity();
    } catch (HttpClientErrorException e) {
      if (isAlreadyExists(e.getStatusCode())) {
        log.debug("Task of message {} already exists", id);
        return;
      }
      throw e;
    }
  }

  private static boolean isAlreadyExists(HttpStatusCode status) {
    return status.value() == 409;
  }
}
