package org.ouracademy.srtomy.iam.query;

import java.util.List;
import org.ouracademy.srtomy.iam.domain.IamRole;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.party.domain.PartyId;

/** An access role as exposed outside the module; {@code churchId} is null for predefined roles. */
public record RoleResult(
    IamRoleId id,
    String name,
    String description,
    List<String> permissions,
    PartyId churchId,
    boolean predefined) {

  public static RoleResult from(IamRole role) {
    return new RoleResult(
        role.getId(),
        role.getName(),
        role.getDescription(),
        List.copyOf(role.getPermissions()),
        role.isPredefined() ? null : role.getChurch().getId(),
        role.isPredefined());
  }
}
