package org.ouracademy.srtomy.domain;

import java.util.UUID;
import org.hibernate.id.uuid.UuidVersion7Strategy;

public final class IdGenerator {

  private IdGenerator() {}

  public static UUID newUuidV7() {
    return UuidVersion7Strategy.INSTANCE.generateUuid(null);
  }
}
