package org.ouracademy.ovejafiel.billing.domain;

import java.util.UUID;
import org.ouracademy.ovejafiel.shared.domain.EntityId;
import org.ouracademy.ovejafiel.shared.domain.IdGenerator;

public record InvoiceId(UUID value) implements EntityId {

  public static InvoiceId generate() {
    return new InvoiceId(IdGenerator.newUuidV7());
  }
}
