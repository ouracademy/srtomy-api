package org.ouracademy.ovejafiel.iam.domain;

import java.util.UUID;
import org.ouracademy.ovejafiel.shared.domain.EntityId;
import org.ouracademy.ovejafiel.shared.domain.IdGenerator;

public record IamRoleId(UUID value) implements EntityId {

  public static IamRoleId generate() {
    return new IamRoleId(IdGenerator.newUuidV7());
  }
}
