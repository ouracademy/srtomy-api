package org.ouracademy.ovejafiel.accountability.domain;

import java.util.Optional;
import java.util.UUID;

public class SystemAccountabilityTypes {

  // Person and Org (like location, group or ministry)
  public static final AccountabilityTypeId VISITOR =
      new AccountabilityTypeId(UUID.fromString("9c89bc94-38b6-4ff6-a08e-2c8f308b462f"));
  public static final AccountabilityTypeId MEMBERSHIP_IN_PROGRESS =
      new AccountabilityTypeId(UUID.fromString("2e3de685-c713-4a33-b862-27672d0710de"));
  public static final AccountabilityTypeId MEMBER =
      new AccountabilityTypeId(UUID.fromString("7ed35dab-dad3-42ca-a3f8-e90a10e1681c"));

  public static final AccountabilityTypeId[] MEMBER_PROGRESS_STATES =
      new AccountabilityTypeId[] {VISITOR, MEMBERSHIP_IN_PROGRESS, MEMBER};

  /** The name of a progress state ({@code VISITOR}, ...), empty for any other type. */
  public static Optional<String> progressStateName(AccountabilityTypeId typeId) {
    if (VISITOR.equals(typeId)) {
      return Optional.of("VISITOR");
    }
    if (MEMBERSHIP_IN_PROGRESS.equals(typeId)) {
      return Optional.of("MEMBERSHIP_IN_PROGRESS");
    }
    if (MEMBER.equals(typeId)) {
      return Optional.of("MEMBER");
    }
    return Optional.empty();
  }

  // Party and Org
  public static final AccountabilityTypeId IS_PART_OF =
      new AccountabilityTypeId(UUID.fromString("1d6e887a-9cd0-461e-b11d-c5d357497ce2"));
  // Person to group/ministry
  public static final AccountabilityTypeId LEADER =
      new AccountabilityTypeId(UUID.fromString("6e0cebd8-4a7a-4ac5-b9dd-658c23708672"));
}
