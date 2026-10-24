package org.ouracademy.srtomy.accountability.command.create_position;

import org.ouracademy.srtomy.party.domain.PartyId;

/** Adds a position named {@code name} to the church {@code churchId}. */
public record CreatePositionCommand(PartyId churchId, String name) {}
