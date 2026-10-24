package org.ouracademy.srtomy.accountability.query;

import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.party.domain.PartyId;

/**
 * A ministerial position (cargo) of a church. {@code accessRoleId} is the access level it gives to
 * whoever holds it, or null when it is only a label.
 */
public record PositionResult(
    AccountabilityTypeId id, String name, PartyId churchId, IamRoleId accessRoleId) {

  public static PositionResult from(AccountabilityType type) {
    return new PositionResult(
        type.getId(), type.getName(), type.getParty().getId(), type.getAccessRoleId());
  }
}
