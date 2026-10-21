package org.ouracademy.srtomy.accountability.command;

import org.ouracademy.srtomy.party.domain.PartyId;

/** Deletes every accountability where the party is either the parent or the child. */
public record DeleteAccountabilitiesForPartyCommand(PartyId partyId) {}
