package org.ouracademy.srtomy.accountability.query;

import java.util.Set;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.party.domain.PartyId;

/**
 * A ministerial position (cargo) of a church. Whoever holds it gets the permissions of {@code
 * roleIds} plus the loose {@code permissions}; with neither it is only a label.
 */
public record PositionResult(
    AccountabilityTypeId id,
    String name,
    PartyId churchId,
    Set<IamRoleId> roleIds,
    Set<String> permissions) {

  public static PositionResult from(AccountabilityType type) {
    return new PositionResult(
        type.getId(),
        type.getName(),
        type.getParty().getId(),
        Set.copyOf(type.getRoleIds()),
        Set.copyOf(type.getPermissions()));
  }
}
