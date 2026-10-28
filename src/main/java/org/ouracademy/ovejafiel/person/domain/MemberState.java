package org.ouracademy.ovejafiel.person.domain;

import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes;

/**
 * How involved a person is with a church, node by node: a visitor, someone who attends and is on
 * the way to membership, or a member. Whoever holds a position is a member.
 */
public enum MemberState {
  VISITOR(SystemAccountabilityTypes.VISITOR),
  MEMBERSHIP_IN_PROGRESS(SystemAccountabilityTypes.MEMBERSHIP_IN_PROGRESS),
  MEMBER(SystemAccountabilityTypes.MEMBER);

  private final AccountabilityTypeId typeId;

  MemberState(AccountabilityTypeId typeId) {
    this.typeId = typeId;
  }

  public AccountabilityTypeId typeId() {
    return typeId;
  }
}
