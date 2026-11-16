package org.ouracademy.ovejafiel.billing.domain;

import java.util.UUID;
import org.ouracademy.ovejafiel.shared.domain.EntityId;
import org.ouracademy.ovejafiel.shared.domain.IdGenerator;

public record AiUsageId(UUID value) implements EntityId {

  public static AiUsageId generate() {
    return new AiUsageId(IdGenerator.newUuidV7());
  }
}
