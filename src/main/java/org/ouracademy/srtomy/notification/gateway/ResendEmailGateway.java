package org.ouracademy.srtomy.notification.gateway;

import java.util.LinkedHashMap;
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
  private final String replyTo;

  ResendEmailGateway(
      @Value("${resend.api-key}") String apiKey,
      @Value("${resend.from:Sr. Tommy <onboarding@resend.dev>}") String from,
      @Value("${resend.url:https://api.resend.com}") String url,
      @Value("${resend.reply-to:}") String replyTo) {
    this.from = from;
    this.replyTo = replyTo;
    this.restClient =
        RestClient.builder()
            .baseUrl(url)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
  }

  @Override
  public void send(String idempotencyKey, String to, String subject, String text) {
    restClient
        .post()
        .uri("/emails")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Idempotency-Key", idempotencyKey)
        .body(body(to, subject, text))
        .retrieve()
        .toBodilessEntity();
  }

  private Map<String, Object> body(String to, String subject, String text) {
    var body = new LinkedHashMap<String, Object>();
    body.put("from", from);
    body.put("to", List.of(to));
    body.put("subject", subject);
    body.put("text", text);
    if (!replyTo.isBlank()) {
      body.put("reply_to", replyTo);
    }
    return body;
  }
}
