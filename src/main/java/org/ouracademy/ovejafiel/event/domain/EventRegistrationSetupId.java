package org.ouracademy.ovejafiel.event.domain;

import java.util.UUID;
import org.ouracademy.ovejafiel.shared.domain.EntityId;
import org.ouracademy.ovejafiel.shared.domain.IdGenerator;

public record EventRegistrationSetupId(UUID value) implements EntityId {

  public static EventRegistrationSetupId generate() {
    return new EventRegistrationSetupId(IdGenerator.newUuidV7());
  }
}
