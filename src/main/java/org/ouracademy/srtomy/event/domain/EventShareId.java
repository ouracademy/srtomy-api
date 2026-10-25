package org.ouracademy.srtomy.event.domain;

import java.util.UUID;
import org.ouracademy.srtomy.shared.domain.EntityId;
import org.ouracademy.srtomy.shared.domain.IdGenerator;

public record EventShareId(UUID value) implements EntityId {

  public static EventShareId generate() {
    return new EventShareId(IdGenerator.newUuidV7());
  }
}
