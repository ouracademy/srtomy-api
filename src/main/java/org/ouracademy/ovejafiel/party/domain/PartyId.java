package org.ouracademy.ovejafiel.party.domain;

import java.util.UUID;
import org.ouracademy.ovejafiel.shared.domain.EntityId;
import org.ouracademy.ovejafiel.shared.domain.IdGenerator;

public record PartyId(UUID value) implements EntityId {

  public static PartyId generate() {
    return new PartyId(IdGenerator.newUuidV7());
  }
}
