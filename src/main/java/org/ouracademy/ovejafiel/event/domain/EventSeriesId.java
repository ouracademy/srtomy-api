package org.ouracademy.ovejafiel.event.domain;

import java.util.UUID;
import org.ouracademy.ovejafiel.shared.domain.EntityId;
import org.ouracademy.ovejafiel.shared.domain.IdGenerator;

public record EventSeriesId(UUID value) implements EntityId {

  public static EventSeriesId generate() {
    return new EventSeriesId(IdGenerator.newUuidV7());
  }
}
