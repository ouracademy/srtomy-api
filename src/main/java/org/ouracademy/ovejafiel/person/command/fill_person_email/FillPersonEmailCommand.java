package org.ouracademy.ovejafiel.person.command.fill_person_email;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** Gives {@code personId} the email {@code email} if the person has none. */
public record FillPersonEmailCommand(PartyId personId, String email) {}
