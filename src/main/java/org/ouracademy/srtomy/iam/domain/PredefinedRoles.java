package org.ouracademy.srtomy.iam.domain;

import java.util.UUID;

/** Ids of the predefined access roles seeded by {@code V1.1.0__identity_iam.sql}. */
public final class PredefinedRoles {

  public static final IamRoleId OWNER = id("0195f0a0-0000-7000-8000-000000000001");
  public static final IamRoleId ADMIN = id("0195f0a0-0000-7000-8000-000000000002");
  public static final IamRoleId LEADER = id("0195f0a0-0000-7000-8000-000000000003");
  public static final IamRoleId COLLABORATOR = id("0195f0a0-0000-7000-8000-000000000004");
  public static final IamRoleId MEMBER = id("0195f0a0-0000-7000-8000-000000000005");

  private PredefinedRoles() {}

  private static IamRoleId id(String value) {
    return new IamRoleId(UUID.fromString(value));
  }
}
