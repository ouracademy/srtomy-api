package org.ouracademy.ovejafiel.user.query;

import org.ouracademy.ovejafiel.user.domain.AppUser;
import org.ouracademy.ovejafiel.user.domain.UserId;

/** A user as exposed outside the module (never the JPA entity). */
public record UserResult(UserId id, String subject, String email, boolean emailVerified) {

  public static UserResult from(AppUser user) {
    return new UserResult(user.getId(), user.getSubject(), user.getEmail(), user.isEmailVerified());
  }
}
