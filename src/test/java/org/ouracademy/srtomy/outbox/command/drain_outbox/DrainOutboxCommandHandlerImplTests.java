package org.ouracademy.srtomy.outbox.command.drain_outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.outbox.OutboxMessageHandler;
import org.ouracademy.srtomy.outbox.domain.OutboxMessage;
import org.ouracademy.srtomy.outbox.domain.OutboxStatus;
import org.ouracademy.srtomy.outbox.repository.OutboxMessageRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class DrainOutboxCommandHandlerImplTests {

  OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
  OutboxMessageHandler emailHandler = mock(OutboxMessageHandler.class);
  DrainOutboxCommandHandlerImpl handler;

  OutboxMessage message(String type) {
    return new OutboxMessage(type, "{\"to\":\"a@x.org\"}", LocalDateTime.now());
  }

  DrainOutboxCommandHandlerImplTests() {
    when(emailHandler.type()).thenReturn("EMAIL");
    var transactions = mock(PlatformTransactionManager.class);
    when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    handler = new DrainOutboxCommandHandlerImpl(repository, transactions, List.of(emailHandler));
  }

  @Test
  void sendsTheDueMessagesUntilThereAreNone() {
    var first = message("EMAIL");
    var second = message("EMAIL");
    when(repository.lockNextDue(any()))
        .thenReturn(Optional.of(first), Optional.of(second), Optional.empty());

    var result = handler.handle(new DrainOutboxCommand(10));

    assertThat(result).isEqualTo(new DrainOutboxResult(2, 0, 0));
    assertThat(first.getStatus()).isEqualTo(OutboxStatus.SENT);
    assertThat(second.getStatus()).isEqualTo(OutboxStatus.SENT);
    verify(emailHandler).handle(first.getId(), first.getPayload());
  }

  @Test
  void neverAttemptsMoreThanTheLimit() {
    when(repository.lockNextDue(any())).thenAnswer(call -> Optional.of(message("EMAIL")));

    var result = handler.handle(new DrainOutboxCommand(3));

    assertThat(result.sent()).isEqualTo(3);
    verify(repository, org.mockito.Mockito.times(3)).lockNextDue(any());
  }

  @Test
  void aFailureLeavesTheMessageForALaterRetryAndGoesOn() {
    var broken = message("EMAIL");
    var fine = message("EMAIL");
    doThrow(new IllegalStateException("403"))
        .when(emailHandler)
        .handle(broken.getId(), broken.getPayload());
    when(repository.lockNextDue(any()))
        .thenReturn(Optional.of(broken), Optional.of(fine), Optional.empty());

    var result = handler.handle(new DrainOutboxCommand(10));

    assertThat(result).isEqualTo(new DrainOutboxResult(1, 1, 0));
    assertThat(broken.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(broken.getAttempts()).isEqualTo(1);
    assertThat(broken.getNextAttemptAt()).isAfter(LocalDateTime.now());
    assertThat(broken.getLastError()).contains("403");
  }

  @Test
  void givesUpWhenTheLastAttemptFails() {
    var broken = message("EMAIL");
    for (int i = 1; i < OutboxMessage.MAX_ATTEMPTS; i++) {
      broken.markFailed("boom", LocalDateTime.now());
    }
    doThrow(new IllegalStateException("403")).when(emailHandler).handle(any(), any());
    when(repository.lockNextDue(any())).thenReturn(Optional.of(broken), Optional.empty());

    var result = handler.handle(new DrainOutboxCommand(10));

    assertThat(result).isEqualTo(new DrainOutboxResult(0, 0, 1));
    assertThat(broken.getStatus()).isEqualTo(OutboxStatus.FAILED);
  }

  @Test
  void aTypeWithoutHandlerIsGivenUpAtOnce() {
    var unknown = message("SMS");
    when(repository.lockNextDue(any())).thenReturn(Optional.of(unknown), Optional.empty());

    var result = handler.handle(new DrainOutboxCommand(10));

    assertThat(result).isEqualTo(new DrainOutboxResult(0, 0, 1));
    assertThat(unknown.getStatus()).isEqualTo(OutboxStatus.FAILED);
    verify(emailHandler, never()).handle(any(), any());
  }

  @Test
  void doesNothingWhenNothingIsDue() {
    when(repository.lockNextDue(any())).thenReturn(Optional.empty());

    assertThat(handler.handle(new DrainOutboxCommand(10)))
        .isEqualTo(new DrainOutboxResult(0, 0, 0));
  }
}
