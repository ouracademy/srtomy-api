package org.ouracademy.srtomy.person;

import java.util.UUID;
import org.ouracademy.srtomy.domain.EntityId;
import org.ouracademy.srtomy.domain.IdGenerator;

public record PartyId(UUID value) implements EntityId {

  public static PartyId generate() {
    return new PartyId(IdGenerator.newUuidV7());
  }
}
