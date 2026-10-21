package org.ouracademy.srtomy.accountability.domain;

import java.util.UUID;
import org.ouracademy.srtomy.shared.domain.EntityId;
import org.ouracademy.srtomy.shared.domain.IdGenerator;

public record AccountabilityId(UUID value) implements EntityId {

  public static AccountabilityId generate() {
    return new AccountabilityId(IdGenerator.newUuidV7());
  }
}
