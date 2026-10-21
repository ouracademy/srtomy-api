package org.ouracademy.srtomy.complaint;

import java.util.UUID;
import org.ouracademy.srtomy.domain.EntityId;
import org.ouracademy.srtomy.domain.IdGenerator;

public record ComplaintId(UUID value) implements EntityId {

  public static ComplaintId generate() {
    return new ComplaintId(IdGenerator.newUuidV7());
  }
}
