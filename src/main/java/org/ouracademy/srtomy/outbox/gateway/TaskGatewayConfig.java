package org.ouracademy.srtomy.outbox.gateway;

import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.ouracademy.srtomy.outbox.command.attempt_delivery.AttemptDeliveryCommandHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cloud Tasks when {@code tasks.project}, {@code tasks.target-url} and {@code
 * internal.scheduler-email} are set (Cloud Run), the local gateway otherwise.
 */
@Slf4j
@Configuration
class TaskGatewayConfig {

  @Bean
  TaskGateway taskGateway(
      AttemptDeliveryCommandHandler attemptDeliveryCommandHandler,
      @Value("${tasks.project:}") String project,
      @Value("${tasks.location:southamerica-west1}") String location,
      @Value("${tasks.target-url:}") String targetUrl,
      @Value("${tasks.api-url:https://cloudtasks.googleapis.com}") String apiUrl,
      @Value("${internal.scheduler-email:}") String serviceAccountEmail,
      @Value("${internal.audience:https://api.srtommy.com/internal}") String audience) {
    if (project.isBlank() || targetUrl.isBlank() || serviceAccountEmail.isBlank()) {
      log.warn(
          "Cloud Tasks is not configured: messages are delivered once, right after the commit,"
              + " and never retried");
      return new LocalTaskGateway(attemptDeliveryCommandHandler);
    }
    return new CloudTasksGateway(
        apiUrl, googleAccessToken(), project, location, targetUrl, serviceAccountEmail, audience);
  }

  /** The runtime service account's token (Cloud Run supplies it), refreshed when it expires. */
  private static Supplier<String> googleAccessToken() {
    return new Supplier<>() {
      private GoogleCredentials credentials;

      @Override
      public synchronized String get() {
        try {
          if (credentials == null) {
            credentials =
                GoogleCredentials.getApplicationDefault()
                    .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
          }
          credentials.refreshIfExpired();
          return credentials.getAccessToken().getTokenValue();
        } catch (IOException e) {
          throw new UncheckedIOException("Could not get a Google access token", e);
        }
      }
    };
  }
}
