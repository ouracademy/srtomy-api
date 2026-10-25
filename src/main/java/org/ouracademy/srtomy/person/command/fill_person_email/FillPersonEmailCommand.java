package org.ouracademy.srtomy.person.command.fill_person_email;

import org.ouracademy.srtomy.party.domain.PartyId;

/** Gives {@code personId} the email {@code email} if the person has none. */
public record FillPersonEmailCommand(PartyId personId, String email) {}
