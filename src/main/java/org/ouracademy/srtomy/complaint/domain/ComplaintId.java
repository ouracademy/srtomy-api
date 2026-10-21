package org.ouracademy.srtomy.complaint.domain;

import java.util.UUID;
import org.ouracademy.srtomy.shared.domain.EntityId;
import org.ouracademy.srtomy.shared.domain.IdGenerator;

public record ComplaintId(UUID value) implements EntityId {

  public static ComplaintId generate() {
    return new ComplaintId(IdGenerator.newUuidV7());
  }
}
