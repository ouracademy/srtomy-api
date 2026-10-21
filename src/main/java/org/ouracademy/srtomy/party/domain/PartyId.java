package org.ouracademy.srtomy.party.domain;

import java.util.UUID;
import org.ouracademy.srtomy.shared.domain.EntityId;
import org.ouracademy.srtomy.shared.domain.IdGenerator;

public record PartyId(UUID value) implements EntityId {

  public static PartyId generate() {
    return new PartyId(IdGenerator.newUuidV7());
  }
}
