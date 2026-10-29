package org.ouracademy.ovejafiel.person.command.restore_person;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * Brings back a person who was removed, as a visitor of {@code organizationId}. What they were
 * before stays as history: they get a new membership, not the old one back.
 */
public record RestorePersonCommand(PartyId personId, PartyId organizationId) {}
