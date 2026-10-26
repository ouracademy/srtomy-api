package org.ouracademy.srtomy.outbox.query.find_outbox_statuses;

import java.util.Map;
import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;
import org.ouracademy.srtomy.outbox.domain.OutboxStatus;

public record FindOutboxStatusesResult(Map<OutboxMessageId, OutboxStatus> statuses) {

  /** Null when the message is not known (for example a row from before the outbox existed). */
  public OutboxStatus of(OutboxMessageId id) {
    return id == null ? null : statuses.get(id);
  }
}
