package org.ouracademy.srtomy.event.domain;

import java.util.UUID;
import org.ouracademy.srtomy.shared.domain.EntityId;
import org.ouracademy.srtomy.shared.domain.IdGenerator;

public record EventRegistrationId(UUID value) implements EntityId {

  public static EventRegistrationId generate() {
    return new EventRegistrationId(IdGenerator.newUuidV7());
  }
}
