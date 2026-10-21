package org.ouracademy.srtomy.person;

import java.util.UUID;
import org.ouracademy.srtomy.shared.domain.EntityId;
import org.ouracademy.srtomy.shared.domain.IdGenerator;

public record AccountabilityTypeId(UUID value) implements EntityId {

  public static AccountabilityTypeId generate() {
    return new AccountabilityTypeId(IdGenerator.newUuidV7());
  }
}
