package org.ouracademy.ovejafiel.iam.domain;

import java.util.UUID;
import org.ouracademy.ovejafiel.shared.domain.EntityId;
import org.ouracademy.ovejafiel.shared.domain.IdGenerator;

public record RoleBindingId(UUID value) implements EntityId {

  public static RoleBindingId generate() {
    return new RoleBindingId(IdGenerator.newUuidV7());
  }
}
