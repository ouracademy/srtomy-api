package org.ouracademy.ovejafiel.iam.query;

import java.util.List;
import org.ouracademy.ovejafiel.iam.domain.IamRole;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.iam.domain.PredefinedRoles;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * An access role as exposed outside the module; {@code churchId} is null for predefined roles.
 * {@code assignable} is false for Owner, which is never the access level of a position.
 */
public record RoleResult(
    IamRoleId id,
    String name,
    String description,
    List<String> permissions,
    PartyId churchId,
    boolean predefined,
    boolean assignable) {

  public static RoleResult from(IamRole role) {
    return new RoleResult(
        role.getId(),
        role.getName(),
        role.getDescription(),
        List.copyOf(role.getPermissions()),
        role.isPredefined() ? null : role.getChurch().getId(),
        role.isPredefined(),
        !role.getId().equals(PredefinedRoles.OWNER));
  }
}
