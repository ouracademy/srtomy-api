package org.ouracademy.ovejafiel.invitation.domain;

import java.util.UUID;
import org.ouracademy.ovejafiel.shared.domain.EntityId;
import org.ouracademy.ovejafiel.shared.domain.IdGenerator;

public record InvitationId(UUID value) implements EntityId {

  public static InvitationId generate() {
    return new InvitationId(IdGenerator.newUuidV7());
  }
}
