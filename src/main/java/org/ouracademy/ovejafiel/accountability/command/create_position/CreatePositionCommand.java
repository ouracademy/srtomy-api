package org.ouracademy.ovejafiel.accountability.command.create_position;

import java.util.Set;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * Adds a position named {@code name} to the church {@code churchId}. It gives the permissions of
 * {@code roleIds} plus the loose {@code permissions}; with neither it is a plain label. Giving
 * permissions is checked against {@code subject}, the user who asks, so nobody creates a position
 * with more access than they hold.
 */
public record CreatePositionCommand(
    String subject,
    PartyId churchId,
    String name,
    Set<IamRoleId> roleIds,
    Set<String> permissions) {}
