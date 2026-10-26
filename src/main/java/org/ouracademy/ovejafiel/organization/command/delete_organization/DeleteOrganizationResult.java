package org.ouracademy.ovejafiel.organization.command.delete_organization;

public record DeleteOrganizationResult(Outcome outcome) {

  public enum Outcome {
    DELETED,
    NOT_FOUND,
    /** Only groups and ministries can be deleted. */
    NOT_DELETABLE
  }
}
