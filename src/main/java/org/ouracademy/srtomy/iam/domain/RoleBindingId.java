package org.ouracademy.srtomy.iam.domain;

import java.util.UUID;
import org.ouracademy.srtomy.shared.domain.EntityId;
import org.ouracademy.srtomy.shared.domain.IdGenerator;

public record RoleBindingId(UUID value) implements EntityId {

  public static RoleBindingId generate() {
    return new RoleBindingId(IdGenerator.newUuidV7());
  }
}
