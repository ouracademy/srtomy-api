package org.ouracademy.ovejafiel.user.domain;

import java.util.UUID;
import org.ouracademy.ovejafiel.shared.domain.EntityId;
import org.ouracademy.ovejafiel.shared.domain.IdGenerator;

public record UserId(UUID value) implements EntityId {

  public static UserId generate() {
    return new UserId(IdGenerator.newUuidV7());
  }
}
