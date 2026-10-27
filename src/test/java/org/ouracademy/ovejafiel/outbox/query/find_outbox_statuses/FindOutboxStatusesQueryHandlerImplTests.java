package org.ouracademy.ovejafiel.outbox.query.find_outbox_statuses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.outbox.domain.OutboxChannel;
import org.ouracademy.ovejafiel.outbox.domain.OutboxMessage;
import org.ouracademy.ovejafiel.outbox.domain.OutboxMessageId;
import org.ouracademy.ovejafiel.outbox.domain.OutboxStatus;
import org.ouracademy.ovejafiel.outbox.repository.OutboxMessageRepository;
import org.ouracademy.ovejafiel.outbox.repository.OutboxMessageStatus;

class FindOutboxStatusesQueryHandlerImplTests {

  OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
  FindOutboxStatusesQueryHandlerImpl handler = new FindOutboxStatusesQueryHandlerImpl(repository);

  @Test
  void answersTheStatusOfEachKnownMessage() {
    var sent = new OutboxMessage(OutboxChannel.EMAIL, "INVITATION", "a@x.org", "{}");
    sent.markSent(null, LocalDateTime.now());
    var waiting = new OutboxMessage(OutboxChannel.EMAIL, "INVITATION", "a@x.org", "{}");
    var unknown = OutboxMessageId.generate();
    when(repository.findStatusesByIdIn(List.of(sent.getId(), waiting.getId(), unknown)))
        .thenReturn(
            List.of(
                new OutboxMessageStatus(sent.getId(), sent.getStatus()),
                new OutboxMessageStatus(waiting.getId(), waiting.getStatus())));

    var result =
        handler.handle(
            new FindOutboxStatusesQuery(List.of(sent.getId(), waiting.getId(), unknown)));

    assertThat(result.of(sent.getId())).isEqualTo(OutboxStatus.SENT);
    assertThat(result.of(waiting.getId())).isEqualTo(OutboxStatus.PENDING);
    assertThat(result.of(unknown)).isNull();
    assertThat(result.of(null)).isNull();
  }

  @Test
  void withoutIdsItDoesNotTouchTheDatabase() {
    var result = handler.handle(new FindOutboxStatusesQuery(Arrays.asList((OutboxMessageId) null)));

    assertThat(result.statuses()).isEmpty();
    verifyNoInteractions(repository);
  }
}
