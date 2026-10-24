package org.ouracademy.srtomy.iam.domain;

import java.util.UUID;

/**
 * Ids of the predefined access roles seeded by {@code V1.1.0__identity_iam.sql} and {@code
 * V1.3.0__positions_with_access.sql}.
 */
public final class PredefinedRoles {

  public static final IamRoleId OWNER = id("0195f0a0-0000-7000-8000-000000000001");
  public static final IamRoleId ADMIN = id("0195f0a0-0000-7000-8000-000000000002");
  public static final IamRoleId LEADER = id("0195f0a0-0000-7000-8000-000000000003");
  public static final IamRoleId COLLABORATOR = id("0195f0a0-0000-7000-8000-000000000004");
  public static final IamRoleId MEMBER = id("0195f0a0-0000-7000-8000-000000000005");
  public static final IamRoleId VIEW_PEOPLE = id("0195f0a0-0000-7000-8000-000000000006");
  public static final IamRoleId PEOPLE_MANAGER = id("0195f0a0-0000-7000-8000-000000000007");
  public static final IamRoleId EVENTS_MANAGER = id("0195f0a0-0000-7000-8000-000000000008");
  public static final IamRoleId ATTENDANCE = id("0195f0a0-0000-7000-8000-000000000009");
  public static final IamRoleId STRUCTURE_MANAGER = id("0195f0a0-0000-7000-8000-00000000000a");

  private PredefinedRoles() {}

  private static IamRoleId id(String value) {
    return new IamRoleId(UUID.fromString(value));
  }
}
