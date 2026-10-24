package org.ouracademy.srtomy.accountability.query;

import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.party.domain.PartyId;

/** A ministerial position (cargo) of a church. */
public record PositionResult(AccountabilityTypeId id, String name, PartyId churchId) {

  public static PositionResult from(AccountabilityType type) {
    return new PositionResult(type.getId(), type.getName(), type.getParty().getId());
  }
}
