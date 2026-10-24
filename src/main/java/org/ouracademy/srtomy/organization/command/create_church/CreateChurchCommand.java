package org.ouracademy.srtomy.organization.command.create_church;

import java.util.List;
import org.ouracademy.srtomy.organization.command.OrganizationInfo;

/**
 * Creates a church together with its first location and initial positions, and makes the signed-in
 * user (identified by {@code subject}) its Owner. All or nothing.
 */
public record CreateChurchCommand(
    OrganizationInfo info,
    OrganizationInfo location,
    List<String> positionNames,
    String subject,
    String email,
    boolean emailVerified) {}
