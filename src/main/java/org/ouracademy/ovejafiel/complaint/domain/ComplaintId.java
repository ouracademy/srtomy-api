package org.ouracademy.ovejafiel.complaint.domain;

import java.util.UUID;
import org.ouracademy.ovejafiel.shared.domain.EntityId;
import org.ouracademy.ovejafiel.shared.domain.IdGenerator;

public record ComplaintId(UUID value) implements EntityId {

  public static ComplaintId generate() {
    return new ComplaintId(IdGenerator.newUuidV7());
  }
}
