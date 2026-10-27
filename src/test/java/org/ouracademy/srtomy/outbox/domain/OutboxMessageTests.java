package org.ouracademy.srtomy.outbox.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class OutboxMessageTests {

  LocalDateTime now = LocalDateTime.of(2026, 10, 26, 10, 0);
  OutboxMessage message =
      new OutboxMessage(OutboxChannel.EMAIL, "INVITATION", "ana@x.org", "{\"link\":\"secret\"}");

  @Test
  void startsPendingWithoutAttempts() {
    assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(message.getAttempts()).isZero();
    assertThat(message.getPayload()).isNotNull();
  }

  @Test
  void sendingItMarksItDoneAndErasesThePayload() {
    message.markSent("re_123", now);

    assertThat(message.getStatus()).isEqualTo(OutboxStatus.SENT);
    assertThat(message.getSentAt()).isEqualTo(now);
    assertThat(message.getProviderMessageId()).isEqualTo("re_123");
    assertThat(message.getAttempts()).isEqualTo(1);
    assertThat(message.getPayload()).isNull();
  }

  @Test
  void aFailedAttemptKeepsItPendingWithItsError() {
    message.recordFailedAttempt("503 from the provider");
    message.recordFailedAttempt("timeout");

    assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(message.getAttempts()).isEqualTo(2);
    assertThat(message.getLastError()).isEqualTo("timeout");
    assertThat(message.getPayload()).isNotNull();
  }

  @Test
  void aLaterSuccessClearsTheError() {
    message.recordFailedAttempt("503");

    message.markSent(null, now);

    assertThat(message.getLastError()).isNull();
    assertThat(message.getProviderMessageId()).isNull();
  }

  @Test
  void givingUpMarksItFailedAndErasesThePayload() {
    message.markFailed("403 domain not verified");

    assertThat(message.getStatus()).isEqualTo(OutboxStatus.FAILED);
    assertThat(message.getLastError()).isEqualTo("403 domain not verified");
    assertThat(message.getPayload()).isNull();
  }

  @Test
  void aLongErrorIsTruncated() {
    message.recordFailedAttempt("x".repeat(5000));

    assertThat(message.getLastError()).hasSize(1000);
  }
}
