package org.ouracademy.srtomy.event.domain;

import java.util.UUID;
import org.ouracademy.srtomy.shared.domain.EntityId;
import org.ouracademy.srtomy.shared.domain.IdGenerator;

public record EventId(UUID value) implements EntityId {

  public static EventId generate() {
    return new EventId(IdGenerator.newUuidV7());
  }
}
