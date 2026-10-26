package org.ouracademy.ovejafiel.accountability.query.get_accountabilities_by_parent_and_types;

import org.ouracademy.ovejafiel.accountability.domain.Accountability;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityId;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.party.domain.PartyId;

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
