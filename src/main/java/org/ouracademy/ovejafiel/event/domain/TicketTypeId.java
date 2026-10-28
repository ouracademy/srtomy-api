package org.ouracademy.ovejafiel.event.domain;

import java.util.UUID;
import org.ouracademy.ovejafiel.shared.domain.EntityId;
import org.ouracademy.ovejafiel.shared.domain.IdGenerator;

public record TicketTypeId(UUID value) implements EntityId {

  public static TicketTypeId generate() {
    return new TicketTypeId(IdGenerator.newUuidV7());
  }
}
