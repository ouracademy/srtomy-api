package org.ouracademy.ovejafiel.iam.query.list_access;

import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.iam.domain.RoleBindingId;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.domain.UserId;

/**
 * One access as seen from a node. {@code inherited} is true when it was made on an ancestor ({@code
 * organizationId} says which one). {@code bindingId} is only set for direct accesses (Owner).
 * {@code positionName} is only set when the access comes from a position, which has no single role:
 * {@code roleId} and {@code roleName} are null then, and {@code permissionCount} says how much it
 * gives.
 */
public record AccessEntryResult(
    RoleBindingId bindingId,
    UserId userId,
    String email,
    IamRoleId roleId,
    String roleName,
    PartyId organizationId,
    String organizationName,
    boolean inherited,
    Source source,
    String positionName,
    int permissionCount) {

  /** Where an access comes from. */
  public enum Source {
    /** A binding made on the node itself; in practice, Owner. */
    DIRECT,
    /** The permissions of a position the user's person holds. */
    POSITION,
    /** Leading a group or ministry. */
    LEADER
  }
}
