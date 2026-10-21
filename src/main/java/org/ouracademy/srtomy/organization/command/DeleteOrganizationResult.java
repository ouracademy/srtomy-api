package org.ouracademy.srtomy.organization.command;

public record DeleteOrganizationResult(Outcome outcome) {

  public enum Outcome {
    DELETED,
    NOT_FOUND,
    /** Only groups and ministries can be deleted. */
    NOT_DELETABLE
  }
}
