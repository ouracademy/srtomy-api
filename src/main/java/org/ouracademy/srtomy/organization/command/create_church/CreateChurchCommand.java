package org.ouracademy.srtomy.organization.command.create_church;

import java.util.List;
import java.util.Set;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.organization.command.OrganizationInfo;

/**
 * Creates a church together with its first location and initial positions, and makes the signed-in
 * user (identified by {@code subject}) its Owner. The user also gets a person record in the first
 * location, holding the position {@code creator.positionName}, linked to their user. All or
 * nothing.
 */
public record CreateChurchCommand(
    OrganizationInfo info,
    OrganizationInfo location,
    List<PositionSeed> positions,
    Creator creator,
    String subject,
    String email,
    boolean emailVerified) {

  /** A position to create; with no roles and no permissions it is a plain label. */
  public record PositionSeed(String name, Set<IamRoleId> roleIds, Set<String> permissions) {}

  /** Who creates the church, as a person of it. */
  public record Creator(String firstName, String lastName, String positionName) {}
}
