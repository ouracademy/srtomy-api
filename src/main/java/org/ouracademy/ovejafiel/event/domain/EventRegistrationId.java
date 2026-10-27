package org.ouracademy.ovejafiel.event.domain;

import java.util.UUID;
import org.ouracademy.ovejafiel.shared.domain.EntityId;
import org.ouracademy.ovejafiel.shared.domain.IdGenerator;

public record EventRegistrationId(UUID value) implements EntityId {

  public static EventRegistrationId generate() {
    return new EventRegistrationId(IdGenerator.newUuidV7());
  }
}
