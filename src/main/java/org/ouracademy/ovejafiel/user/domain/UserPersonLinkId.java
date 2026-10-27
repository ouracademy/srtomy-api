package org.ouracademy.ovejafiel.user.domain;

import java.util.UUID;
import org.ouracademy.ovejafiel.shared.domain.EntityId;
import org.ouracademy.ovejafiel.shared.domain.IdGenerator;

public record UserPersonLinkId(UUID value) implements EntityId {

  public static UserPersonLinkId generate() {
    return new UserPersonLinkId(IdGenerator.newUuidV7());
  }
}
