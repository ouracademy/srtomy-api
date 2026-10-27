package org.ouracademy.ovejafiel.accountability.domain;

import java.util.UUID;
import org.ouracademy.ovejafiel.shared.domain.EntityId;
import org.ouracademy.ovejafiel.shared.domain.IdGenerator;

public record AccountabilityId(UUID value) implements EntityId {

  public static AccountabilityId generate() {
    return new AccountabilityId(IdGenerator.newUuidV7());
  }
}
