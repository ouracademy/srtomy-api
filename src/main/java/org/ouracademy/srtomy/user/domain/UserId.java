package org.ouracademy.srtomy.user.domain;

import java.util.UUID;
import org.ouracademy.srtomy.shared.domain.EntityId;
import org.ouracademy.srtomy.shared.domain.IdGenerator;

public record UserId(UUID value) implements EntityId {

  public static UserId generate() {
    return new UserId(IdGenerator.newUuidV7());
  }
}
