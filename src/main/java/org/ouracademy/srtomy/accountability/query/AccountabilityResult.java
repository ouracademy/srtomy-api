package org.ouracademy.srtomy.accountability.query;

import org.ouracademy.srtomy.accountability.domain.Accountability;
import org.ouracademy.srtomy.accountability.domain.AccountabilityId;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.party.domain.PartyId;

/** An accountability (child is &lt;type&gt; of parent) as exposed to other modules. */
public record AccountabilityResult(
    AccountabilityId id,
    PartyId childId,
    String childName,
    PartyId parentId,
    AccountabilityTypeId typeId,
    String typeName) {

  static AccountabilityResult from(Accountability accountability) {
    return new AccountabilityResult(
        accountability.getId(),
        accountability.getChild().getId(),
        accountability.getChild().getName(),
        accountability.getParent().getId(),
        accountability.getType().getId(),
        accountability.getType().getName());
  }
}
