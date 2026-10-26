package org.ouracademy.ovejafiel.notification.gateway;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Sends through Resend's HTTP API (https://resend.com/docs/api-reference/emails/send-email). */
@Component
@ConditionalOnExpression("!'${resend.api-key:}'.isEmpty()")
class ResendEmailGateway implements EmailGateway {

  private final RestClient restClient;
  private final String from;

  ResendEmailGateway(
      @Value("${resend.api-key}") String apiKey,
      @Value("${resend.from:Sr. Tommy <onboarding@resend.dev>}") String from,
      @Value("${resend.url:https://api.resend.com}") String url) {
    this.from = from;
    this.restClient =
        RestClient.builder()
            .baseUrl(url)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
  }

  @Override
  public void send(String to, String subject, String text) {
    restClient
        .post()
        .uri("/emails")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("from", from, "to", List.of(to), "subject", subject, "text", text))
        .retrieve()
        .toBodilessEntity();
  }
}
