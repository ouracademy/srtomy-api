package org.ouracademy.ovejafiel.event.domain;

import java.util.UUID;
import org.ouracademy.ovejafiel.shared.domain.EntityId;
import org.ouracademy.ovejafiel.shared.domain.IdGenerator;

public record EventId(UUID value) implements EntityId {

  public static EventId generate() {
    return new EventId(IdGenerator.newUuidV7());
  }
}
