package org.ouracademy.srtomy.user.query;

import org.ouracademy.srtomy.user.domain.AppUser;
import org.ouracademy.srtomy.user.domain.UserId;

/** A user as exposed outside the module (never the JPA entity). */
public record UserResult(UserId id, String subject, String email, boolean emailVerified) {

  public static UserResult from(AppUser user) {
    return new UserResult(user.getId(), user.getSubject(), user.getEmail(), user.isEmailVerified());
  }
}
