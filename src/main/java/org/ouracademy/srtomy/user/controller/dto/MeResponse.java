package org.ouracademy.srtomy.user.controller.dto;

import java.util.UUID;
import org.ouracademy.srtomy.user.query.UserResult;

public record MeResponse(UUID id, String email, boolean emailVerified) {

  public static MeResponse from(UserResult user) {
    return new MeResponse(user.id().value(), user.email(), user.emailVerified());
  }
}
