package org.ouracademy.srtomy.invitation.domain;

import java.util.UUID;
import org.ouracademy.srtomy.shared.domain.EntityId;
import org.ouracademy.srtomy.shared.domain.IdGenerator;

public record InvitationId(UUID value) implements EntityId {

  public static InvitationId generate() {
    return new InvitationId(IdGenerator.newUuidV7());
  }
}
