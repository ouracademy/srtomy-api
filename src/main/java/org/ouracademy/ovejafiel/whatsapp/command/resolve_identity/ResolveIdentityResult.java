package org.ouracademy.ovejafiel.whatsapp.command.resolve_identity;

import java.time.Instant;
import java.util.UUID;

/**
 * @param token only for {@link Status#LINKED}: a short-lived token to call the API as that person
 */
public record ResolveIdentityResult(
    Status status, String token, Instant expiresAt, UUID personId, UUID churchId, String name) {

  public enum Status {
    /** The person is known: the token has their rights (Member level, nothing more). */
    LINKED,
    /** The number they wrote to is not a connected church number. */
    CHURCH_NOT_CONNECTED,
    /** No active person of the church has that phone. */
    UNKNOWN,
    /** Several people share that phone: the agent cannot tell who writes. */
    AMBIGUOUS,
    /** The person has a real account: its rights need the phone to be verified from the portal. */
    NEEDS_VERIFICATION
  }

  static ResolveIdentityResult of(Status status) {
    return new ResolveIdentityResult(status, null, null, null, null, null);
  }
}
