package org.ouracademy.ovejafiel.outbox.domain;

import java.util.UUID;
import org.ouracademy.ovejafiel.shared.domain.EntityId;
import org.ouracademy.ovejafiel.shared.domain.IdGenerator;

public record OutboxMessageId(UUID value) implements EntityId {

  public static OutboxMessageId generate() {
    return new OutboxMessageId(IdGenerator.newUuidV7());
  }
}
