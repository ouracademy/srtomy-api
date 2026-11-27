package org.ouracademy.ovejafiel.outbox.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.LocalDateTime;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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
  void aWaitingMessageCanBeCancelledAndItsPayloadIsErased() {
    assertThat(message.cancel()).isTrue();

    assertThat(message.getStatus()).isEqualTo(OutboxStatus.CANCELLED);
    assertThat(message.getPayload()).isNull();
  }

  @Test
  void aMessageThatAlreadyLeftCannotBeCancelled() {
    message.markSent("re_1", now);

    assertThat(message.cancel()).isFalse();
    assertThat(message.getStatus()).isEqualTo(OutboxStatus.SENT);
  }

  @Test
  void aMessageGivenUpStaysFailed() {
    message.markFailed("403");

    assertThat(message.cancel()).isFalse();
    assertThat(message.getStatus()).isEqualTo(OutboxStatus.FAILED);
  }

  @Test
  void aLongErrorIsTruncated() {
    message.recordFailedAttempt("x".repeat(5000));

    assertThat(message.getLastError()).hasSize(1000);
  }

  /** Every way to change a message, so a new one cannot skip the table below. */
  private static final java.util.Map<String, Consumer<OutboxMessage>> MOVES =
      java.util.Map.of(
          "send", m -> m.markSent("re_1", LocalDateTime.of(2026, 10, 26, 10, 0)),
          "attempt", m -> m.recordFailedAttempt("503"),
          "fail", m -> m.markFailed("403"));

  private OutboxMessage in(OutboxStatus status) {
    var m = new OutboxMessage(OutboxChannel.EMAIL, "INVITATION", "ana@x.org", "{}");
    switch (status) {
      case PENDING -> {}
      case SENT -> m.markSent("re_0", now);
      case FAILED -> m.markFailed("403");
      case CANCELLED -> m.cancel();
    }
    return m;
  }

  @Test
  void aWaitingMessageAcceptsEveryMove() {
    MOVES.values().forEach(move -> move.accept(in(OutboxStatus.PENDING)));
  }

  @ParameterizedTest
  @EnumSource(
      value = OutboxStatus.class,
      names = {"SENT", "FAILED", "CANCELLED"})
  void aFinalMessageRefusesEveryMove(OutboxStatus final_) {
    MOVES.forEach(
        (name, move) -> {
          var m = in(final_);
          assertThatIllegalStateException()
              .as("%s from %s", name, final_)
              .isThrownBy(() -> move.accept(m));
          assertThat(m.getStatus()).isEqualTo(final_);
        });
  }
}
