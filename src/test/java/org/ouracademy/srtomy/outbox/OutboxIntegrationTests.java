package org.ouracademy.srtomy.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.notification.SentEmails;
import org.ouracademy.srtomy.notification.TestEmailConfig;
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
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(classes = SrtomyApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({TestSecurityConfig.class, TestEmailConfig.class})
class OutboxIntegrationTests {

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired OutboxMessageRepository repository;
  @Autowired SentEmails sentEmails;
  RestTestClient client;

  @BeforeEach
  void setUp() {
    sentEmails.reset();
    client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  RestTestClient.ResponseSpec drain(String token) {
    return client
        .post()
        .uri("/v1/internal/outbox/drain")
        .headers(
            headers -> {
              if (token != null) {
                headers.setBearerAuth(token);
              }
            })
        .exchange();
  }

  OutboxMessage queue(LocalDateTime dueAt) {
    return repository.save(
        new OutboxMessage(
            "EMAIL", "{\"to\":\"ana@x.org\",\"subject\":\"Hola\",\"text\":\"Texto\"}", dueAt));
  }

  @Test
  void theSchedulerDrainsTheDueMessages() {
    var due = queue(LocalDateTime.now().minusMinutes(1));

    drain(TestSecurityConfig.schedulerToken(jwtEncoder)).expectStatus().isOk();

    assertThat(sentEmails.all())
        .anyMatch(
            email ->
                email.idempotencyKey().equals(due.getId().value().toString())
                    && email.to().equals("ana@x.org"));
    assertThat(repository.findById(due.getId()).orElseThrow().getStatus())
        .isEqualTo(OutboxStatus.SENT);
  }

  @Test
  void aMessageNotYetDueWaits() {
    var later = queue(LocalDateTime.now().plusHours(1));

    drain(TestSecurityConfig.schedulerToken(jwtEncoder)).expectStatus().isOk();

    assertThat(sentEmails.all())
        .noneMatch(email -> email.idempotencyKey().equals(later.getId().value().toString()));
    assertThat(repository.findById(later.getId()).orElseThrow().getStatus())
        .isEqualTo(OutboxStatus.PENDING);
  }

  @Test
  void aFailingProviderLeavesTheMessageForTheNextAttempt() {
    sentEmails.failWith(new IllegalStateException("403"));
    var due = queue(LocalDateTime.now().minusMinutes(1));

    drain(TestSecurityConfig.schedulerToken(jwtEncoder)).expectStatus().isOk();

    var after = repository.findById(due.getId()).orElseThrow();
    assertThat(after.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(after.getAttempts()).isEqualTo(1);
    assertThat(after.getNextAttemptAt()).isAfter(LocalDateTime.now());
  }

  @Test
  void withoutATokenItIs401() {
    drain(null).expectStatus().isUnauthorized();
  }

  @Test
  void anAuth0TokenOfAUserIs401() {
    drain(TestSecurityConfig.validToken(jwtEncoder)).expectStatus().isUnauthorized();
  }

  @Test
  void aGoogleTokenOfAnotherAccountIs401() {
    drain(
            TestSecurityConfig.googleToken(
                jwtEncoder,
                "someone@else.iam.gserviceaccount.com",
                true,
                TestSecurityConfig.INTERNAL_AUDIENCE))
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void anUnverifiedEmailIs401() {
    drain(
            TestSecurityConfig.googleToken(
                jwtEncoder,
                TestSecurityConfig.SCHEDULER_EMAIL,
                false,
                TestSecurityConfig.INTERNAL_AUDIENCE))
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void aTokenForAnotherAudienceIs401() {
    drain(
            TestSecurityConfig.googleToken(
                jwtEncoder,
                TestSecurityConfig.SCHEDULER_EMAIL,
                true,
                "https://elsewhere.example.com"))
        .expectStatus()
        .isUnauthorized();
  }
}
