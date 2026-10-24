package org.ouracademy.srtomy.accountability.command.create_position;

import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.party.domain.PartyId;

/**
 * Adds a position named {@code name} to the church {@code churchId}. {@code accessRoleId} is its
 * access level (null for a plain label); giving one is checked against {@code subject}, the user
 * who asks, so nobody creates a position with more access than they hold.
 */
public record CreatePositionCommand(
    String subject, PartyId churchId, String name, IamRoleId accessRoleId) {}
