package org.ouracademy.srtomy.iam.domain;

import java.util.UUID;
import org.ouracademy.srtomy.shared.domain.EntityId;
import org.ouracademy.srtomy.shared.domain.IdGenerator;

public record IamRoleId(UUID value) implements EntityId {

  public static IamRoleId generate() {
    return new IamRoleId(IdGenerator.newUuidV7());
  }
}
