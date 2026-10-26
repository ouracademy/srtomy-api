package org.ouracademy.srtomy.event.domain;

import java.util.UUID;
import org.ouracademy.srtomy.shared.domain.EntityId;
import org.ouracademy.srtomy.shared.domain.IdGenerator;

public record EventSeriesId(UUID value) implements EntityId {

  public static EventSeriesId generate() {
    return new EventSeriesId(IdGenerator.newUuidV7());
  }
}
