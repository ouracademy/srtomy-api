package org.ouracademy.ovejafiel.iam.command.add_owner;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * The user with {@code subject} makes the user with this email an Owner of the church that {@code
 * organizationId} belongs to. Only an Owner can.
 */
public record AddOwnerCommand(String subject, String email, PartyId organizationId) {}
