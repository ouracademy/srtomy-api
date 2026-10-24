package org.ouracademy.srtomy.user.domain;

import java.util.UUID;
import org.ouracademy.srtomy.shared.domain.EntityId;
import org.ouracademy.srtomy.shared.domain.IdGenerator;

public record UserPersonLinkId(UUID value) implements EntityId {

  public static UserPersonLinkId generate() {
    return new UserPersonLinkId(IdGenerator.newUuidV7());
  }
}
