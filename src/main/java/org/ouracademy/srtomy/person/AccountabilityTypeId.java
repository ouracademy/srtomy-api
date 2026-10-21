package org.ouracademy.srtomy.person;

import java.util.UUID;
import org.ouracademy.srtomy.domain.EntityId;
import org.ouracademy.srtomy.domain.IdGenerator;

public record AccountabilityTypeId(UUID value) implements EntityId {

  public static AccountabilityTypeId generate() {
    return new AccountabilityTypeId(IdGenerator.newUuidV7());
  }
}
