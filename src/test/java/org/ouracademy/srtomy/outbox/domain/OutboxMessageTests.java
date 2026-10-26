package org.ouracademy.srtomy.outbox.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class OutboxMessageTests {

  LocalDateTime now = LocalDateTime.of(2026, 10, 26, 10, 0);
  OutboxMessage message = new OutboxMessage("EMAIL", "{}", now);

  @Test
  void startsPendingAndDueAtOnce() {
    assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(message.getNextAttemptAt()).isEqualTo(now);
    assertThat(message.getAttempts()).isZero();
  }

  @Test
  void sendingItMarksItDone() {
    message.markSent(now);

    assertThat(message.getStatus()).isEqualTo(OutboxStatus.SENT);
    assertThat(message.getSentAt()).isEqualTo(now);
    assertThat(message.getAttempts()).isEqualTo(1);
  }

  @Test
  void eachFailureWaitsLongerBeforeTheNextAttempt() {
    var waits =
        new Duration[] {
          Duration.ofMinutes(10),
          Duration.ofMinutes(30),
          Duration.ofHours(2),
          Duration.ofHours(6),
          Duration.ofHours(12)
        };
    for (var wait : waits) {
      assertThat(message.markFailed("boom", now)).isEqualTo(OutboxStatus.PENDING);
      assertThat(message.getNextAttemptAt()).isEqualTo(now.plus(wait));
    }
    assertThat(message.getLastError()).isEqualTo("boom");
  }

  @Test
  void givesUpAfterTheLastAttempt() {
    for (int i = 1; i < OutboxMessage.MAX_ATTEMPTS; i++) {
      message.markFailed("boom", now);
    }

    assertThat(message.markFailed("still boom", now)).isEqualTo(OutboxStatus.FAILED);
    assertThat(message.getAttempts()).isEqualTo(OutboxMessage.MAX_ATTEMPTS);
  }

  @Test
  void aLongErrorIsTruncated() {
    message.markFailed("x".repeat(5000), now);

    assertThat(message.getLastError()).hasSize(1000);
  }

  @Test
  void canGiveUpAtOnce() {
    message.giveUp("No handler");

    assertThat(message.getStatus()).isEqualTo(OutboxStatus.FAILED);
    assertThat(message.getLastError()).isEqualTo("No handler");
  }
}
