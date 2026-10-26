package org.ouracademy.ovejafiel.iam.query.authorize;

import java.util.Collection;
import java.util.List;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * Asks whether the user identified by {@code subject} holds {@code permission} on at least one of
 * {@code organizationIds} (directly or inherited from an ancestor).
 */
public record AuthorizeQuery(
    String subject, Collection<PartyId> organizationIds, Permission permission) {

  public AuthorizeQuery(String subject, PartyId organizationId, Permission permission) {
    this(subject, List.of(organizationId), permission);
  }
}
