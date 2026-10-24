package org.ouracademy.srtomy.iam.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * Catalog of everything an access role can allow, GCP-style {@code resource.action}. The wire value
 * is what roles store and what the API returns.
 */
public enum Permission {
  ORG_READ("org.read"),
  ORG_UPDATE("org.update"),
  ORG_CREATE_CHILD("org.create_child"),
  ORG_DELETE("org.delete"),
  MEMBERS_READ("members.read"),
  MEMBERS_WRITE("members.write"),
  EVENTS_READ("events.read"),
  EVENTS_WRITE("events.write"),
  ATTENDANCE_WRITE("attendance.write"),
  POSITIONS_MANAGE("positions.manage"),
  IAM_READ("iam.read"),
  IAM_MANAGE("iam.manage"),
  INVITATIONS_SEND("invitations.send");

  private final String value;

  Permission(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }

  public static Optional<Permission> fromValue(String value) {
    return Arrays.stream(values()).filter(p -> p.value.equals(value)).findFirst();
  }
}
