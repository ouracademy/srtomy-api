package org.ouracademy.srtomy.event;

import java.util.UUID;
import org.ouracademy.srtomy.domain.EntityId;
import org.ouracademy.srtomy.domain.IdGenerator;

public record EventId(UUID value) implements EntityId {

  public static EventId generate() {
    return new EventId(IdGenerator.newUuidV7());
  }
}
