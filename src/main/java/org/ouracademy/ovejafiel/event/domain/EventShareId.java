package org.ouracademy.ovejafiel.event.domain;

import java.util.UUID;
import org.ouracademy.ovejafiel.shared.domain.EntityId;
import org.ouracademy.ovejafiel.shared.domain.IdGenerator;

public record EventShareId(UUID value) implements EntityId {

  public static EventShareId generate() {
    return new EventShareId(IdGenerator.newUuidV7());
  }
}
