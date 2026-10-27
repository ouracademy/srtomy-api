package org.ouracademy.srtomy.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.notification.SentEmails;
import org.ouracademy.srtomy.notification.TestEmailConfig;
import org.ouracademy.srtomy.outbox.domain.OutboxChannel;
import org.ouracademy.srtomy.outbox.domain.OutboxMessage;
import org.ouracademy.srtomy.outbox.domain.OutboxStatus;
import org.ouracademy.srtomy.outbox.repository.OutboxMessageRepository;
import org.ouracademy.srtomy.shared.SrtomyApplication;
import org.ouracademy.srtomy.shared.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Cloud Tasks calls the attempt endpoint and Cloud Scheduler the sweep, with the scheduler's token.
 */
@SpringBootTest(classes = SrtomyApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({TestSecurityConfig.class, TestEmailConfig.class})
class OutboxIntegrationTests {

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired OutboxMessageRepository repository;
  @Autowired SentEmails sentEmails;
  @Autowired JdbcTemplate jdbc;
  RestTestClient client;

  @BeforeEach
  void setUp() {
    sentEmails.reset();
    client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  RestTestClient.ResponseSpec post(String path, String token) {
    return client
        .post()
        .uri(path)
        .headers(
            headers -> {
              if (token != null) {
                headers.setBearerAuth(token);
              }
            })
        .exchange();
  }

  String scheduler() {
    return TestSecurityConfig.schedulerToken(jwtEncoder);
  }

  /** Saved without a task, like a bulk message: only the endpoints below deliver it. */
  OutboxMessage queued() {
    return repository.save(
        new OutboxMessage(
            OutboxChannel.EMAIL,
            "INVITATION",
            "ana@x.org",
            "{\"subject\":\"Hola\",\"text\":\"Texto\"}"));
  }

  String attemptPath(OutboxMessage message) {
    return "/v1/internal/outbox/" + message.getId().value() + "/attempt";
  }

  OutboxMessage reload(OutboxMessage message) {
    return repository.findById(message.getId()).orElseThrow();
  }

  @Test
  void anAttemptDeliversTheMessageAndErasesItsPayload() {
    var message = queued();

    post(attemptPath(message), scheduler()).expectStatus().isOk();

    assertThat(sentEmails.all())
        .anyMatch(
            email ->
                email.idempotencyKey().equals(message.getId().value().toString())
                    && email.to().equals("ana@x.org")
                    && email.subject().equals("Hola"));
    var after = reload(message);
    assertThat(after.getStatus()).isEqualTo(OutboxStatus.SENT);
    assertThat(after.getPayload()).isNull();
    assertThat(after.getProviderMessageId()).isEqualTo("test-" + message.getId().value());
  }

  @Test
  void aRepeatedTaskDoesNotSendTwice() {
    var message = queued();

    post(attemptPath(message), scheduler()).expectStatus().isOk();
    post(attemptPath(message), scheduler()).expectStatus().isOk();

    assertThat(sentEmails.all())
        .filteredOn(email -> email.idempotencyKey().equals(message.getId().value().toString()))
        .hasSize(1);
  }

  @Test
  void aTransientFailureAnswers503SoTheQueueCallsAgain() {
    sentEmails.failWith(new IllegalStateException("503 from the provider"));
    var message = queued();

    post(attemptPath(message), scheduler()).expectStatus().isEqualTo(503);

    var after = reload(message);
    assertThat(after.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(after.getAttempts()).isEqualTo(1);
    assertThat(after.getLastError()).contains("503");
  }

  @Test
  void aPermanentFailureGivesUpAndAnswers200SoTheQueueStops() {
    sentEmails.failWith(new PermanentDeliveryException("403 domain not verified", null));
    var message = queued();

    post(attemptPath(message), scheduler()).expectStatus().isOk();

    var after = reload(message);
    assertThat(after.getStatus()).isEqualTo(OutboxStatus.FAILED);
    assertThat(after.getPayload()).isNull();
  }

  @Test
  void aMessageThatDoesNotExistEndsTheTask() {
    post("/v1/internal/outbox/" + java.util.UUID.randomUUID() + "/attempt", scheduler())
        .expectStatus()
        .isOk();
  }

  @Test
  void theSweepSchedulesTheMessagesNothingTouchedForAWhile() {
    var old = queued();
    var fresh = queued();
    jdbc.update(
        "update outbox_message set last_modified_date = ? where id = ?",
        LocalDateTime.now().minusHours(2),
        old.getId().value());

    post("/v1/internal/outbox/dispatch-pending", scheduler()).expectStatus().isOk();

    assertThat(reload(old).getStatus()).isEqualTo(OutboxStatus.SENT);
    assertThat(reload(fresh).getStatus()).isEqualTo(OutboxStatus.PENDING);
  }

  @Test
  void withoutATokenItIs401() {
    post(attemptPath(queued()), null).expectStatus().isUnauthorized();
    post("/v1/internal/outbox/dispatch-pending", null).expectStatus().isUnauthorized();
  }

  @Test
  void anAuth0TokenOfAUserIs401() {
    post(attemptPath(queued()), TestSecurityConfig.validToken(jwtEncoder))
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void aGoogleTokenOfAnotherAccountIs401() {
    var token =
        TestSecurityConfig.googleToken(
            jwtEncoder,
            "someone@else.iam.gserviceaccount.com",
            true,
            TestSecurityConfig.INTERNAL_AUDIENCE);

    post("/v1/internal/outbox/dispatch-pending", token).expectStatus().isUnauthorized();
  }

  @Test
  void anUnverifiedEmailIs401() {
    var token =
        TestSecurityConfig.googleToken(
            jwtEncoder,
            TestSecurityConfig.SCHEDULER_EMAIL,
            false,
            TestSecurityConfig.INTERNAL_AUDIENCE);

    post("/v1/internal/outbox/dispatch-pending", token).expectStatus().isUnauthorized();
  }

  @Test
  void aTokenForAnotherAudienceIs401() {
    var token =
        TestSecurityConfig.googleToken(
            jwtEncoder, TestSecurityConfig.SCHEDULER_EMAIL, true, "https://elsewhere.example.com");

    post("/v1/internal/outbox/dispatch-pending", token).expectStatus().isUnauthorized();
  }
}
