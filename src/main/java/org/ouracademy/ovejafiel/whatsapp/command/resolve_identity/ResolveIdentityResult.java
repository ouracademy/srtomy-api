package org.ouracademy.ovejafiel.whatsapp.command.resolve_identity;

import java.time.Instant;
import java.util.UUID;

/**
 * @param token only for {@link Status#LINKED}: a short-lived token to call the API as that person
 * @param churchSlug the slug of the public page of the church, set whenever the number belongs to a
 *     connected church: anyone may ask about what that page shows, known or not
 * @param name only for {@link Status#LINKED}: the name of the person
 * @param level only for {@link Status#LINKED}: how far the token reaches
 */
public record ResolveIdentityResult(
    Status status,
    String token,
    Instant expiresAt,
    UUID personId,
    UUID churchId,
    String churchName,
    String churchSlug,
    String name,
    Level level) {

  public enum Level {
    /** Known by the phone on their record: the Member level and nothing more. */
    MEMBER,
    /** Proved the number from their account: everything their account can do. */
    ACCOUNT
  }

  public enum Status {
    /** The person is known: the token has their rights (Member level, nothing more). */
    LINKED,
    /** The number they wrote to is not a connected church number. */
    CHURCH_NOT_CONNECTED,
    /** No active person of the church has that phone: they may still ask about the church. */
    UNKNOWN,
    /** Several people share that phone: the agent cannot tell who writes. */
    AMBIGUOUS,
    /** The person has an account: its rights need the number to be linked from the portal. */
    NEEDS_VERIFICATION
  }

  /** Nobody is identified: no token, no person. */
  static ResolveIdentityResult of(Status status) {
    return new ResolveIdentityResult(status, null, null, null, null, null, null, null, null);
  }

  /** The church is known but the person is not (or cannot be taken for sure). */
  static ResolveIdentityResult ofChurch(
      Status status, UUID churchId, String churchName, String churchSlug) {
    return new ResolveIdentityResult(
        status, null, null, null, churchId, churchName, churchSlug, null, null);
  }
}
