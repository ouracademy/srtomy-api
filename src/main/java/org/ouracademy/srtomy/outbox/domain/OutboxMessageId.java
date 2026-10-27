package org.ouracademy.srtomy.outbox.domain;

import java.util.UUID;
import org.ouracademy.srtomy.shared.domain.EntityId;
import org.ouracademy.srtomy.shared.domain.IdGenerator;

public record OutboxMessageId(UUID value) implements EntityId {

  public static OutboxMessageId generate() {
    return new OutboxMessageId(IdGenerator.newUuidV7());
  }
}
