package org.ouracademy.ovejafiel.person.domain;

import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes;

/**
 * How involved a person is with a church, node by node: a visitor, someone who attends and is on
 * the way to membership, or a member. Whoever holds a position is a member.
 */
public enum MemberState {
  VISITOR(SystemAccountabilityTypes.VISITOR, 1),
  MEMBERSHIP_IN_PROGRESS(SystemAccountabilityTypes.MEMBERSHIP_IN_PROGRESS, 2),
  MEMBER(SystemAccountabilityTypes.MEMBER, 3);

  private final AccountabilityTypeId typeId;
  private final int rank;

  MemberState(AccountabilityTypeId typeId, int rank) {
    this.typeId = typeId;
    this.rank = rank;
  }

  /** 1 for a visitor up to 3 for a member: a person in several nodes counts with the highest. */
  public int rank() {
    return rank;
  }

  public AccountabilityTypeId typeId() {
    return typeId;
  }
}
