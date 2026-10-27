package org.ouracademy.ovejafiel.outbox.command.dispatch_pending;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.outbox.domain.OutboxChannel;
import org.ouracademy.ovejafiel.outbox.domain.OutboxMessage;
import org.ouracademy.ovejafiel.outbox.gateway.TaskGateway;
import org.ouracademy.ovejafiel.outbox.repository.OutboxMessageRepository;

class DispatchPendingCommandHandlerImplTests {

  OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
  TaskGateway tasks = mock(TaskGateway.class);
  DispatchPendingCommandHandlerImpl handler =
      new DispatchPendingCommandHandlerImpl(repository, tasks);

  OutboxMessage message(OutboxChannel channel) {
    return new OutboxMessage(channel, "BIRTHDAY", "someone", "{}");
  }

  @Test
  void schedulesEachPendingMessageOnItsOwnChannel() {
    var email = message(OutboxChannel.EMAIL);
    var whatsapp = message(OutboxChannel.WHATSAPP);
    when(repository.findPendingUntouchedSince(any(), any())).thenReturn(List.of(email, whatsapp));

    var result = handler.handle(new DispatchPendingCommand(Duration.ofMinutes(15), 100));

    assertThat(result).isEqualTo(new DispatchPendingResult(2, 0));
    verify(tasks).schedule(email.getId(), OutboxChannel.EMAIL);
    verify(tasks).schedule(whatsapp.getId(), OutboxChannel.WHATSAPP);
  }

  @Test
  void oneFailureDoesNotStopTheRest() {
    var broken = message(OutboxChannel.EMAIL);
    var fine = message(OutboxChannel.EMAIL);
    when(repository.findPendingUntouchedSince(any(), any())).thenReturn(List.of(broken, fine));
    doThrow(new IllegalStateException("cloud tasks down"))
        .when(tasks)
        .schedule(broken.getId(), OutboxChannel.EMAIL);

    var result = handler.handle(new DispatchPendingCommand(Duration.ofMinutes(15), 100));

    assertThat(result).isEqualTo(new DispatchPendingResult(1, 1));
    verify(tasks).schedule(fine.getId(), OutboxChannel.EMAIL);
  }

  @Test
  void doesNothingWhenNothingIsPending() {
    when(repository.findPendingUntouchedSince(any(), any())).thenReturn(List.of());

    assertThat(handler.handle(new DispatchPendingCommand(Duration.ofMinutes(15), 100)))
        .isEqualTo(new DispatchPendingResult(0, 0));
  }
}
