package org.ouracademy.srtomy.outbox.command.drain_outbox;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.outbox.OutboxMessageEnqueued;

class DrainOnCommitTests {

  DrainOutboxCommandHandler drain = mock(DrainOutboxCommandHandler.class);
  DrainOnCommit listener = new DrainOnCommit(drain);

  @Test
  void drainsAfterTheCommit() {
    listener.onEnqueued(new OutboxMessageEnqueued());

    verify(drain).handle(new DrainOutboxCommand(10));
  }

  @Test
  void aFailureNeverReachesTheRequestThatEnqueued() {
    when(drain.handle(any())).thenThrow(new IllegalStateException("db down"));

    assertThatCode(() -> listener.onEnqueued(new OutboxMessageEnqueued()))
        .doesNotThrowAnyException();
  }
}
