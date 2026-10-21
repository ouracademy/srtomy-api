package org.ouracademy.srtomy.accountability.query;

import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;

/** An accountability type (e.g. a role) as exposed to other modules. */
public record AccountabilityTypeResult(AccountabilityTypeId id, String name, boolean predefined) {

  public static AccountabilityTypeResult from(AccountabilityType type) {
    return new AccountabilityTypeResult(type.getId(), type.getName(), type.isPredefined());
  }
}
