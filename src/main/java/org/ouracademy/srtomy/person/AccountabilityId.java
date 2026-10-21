package org.ouracademy.srtomy.person;

import java.util.UUID;
import org.ouracademy.srtomy.domain.EntityId;
import org.ouracademy.srtomy.domain.IdGenerator;

public record AccountabilityId(UUID value) implements EntityId {

  public static AccountabilityId generate() {
    return new AccountabilityId(IdGenerator.newUuidV7());
  }
}
