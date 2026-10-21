package org.ouracademy.srtomy.accountability.command.delete_accountabilities_for_party;

import org.ouracademy.srtomy.party.domain.PartyId;

/** Deletes every accountability where the party is either the parent or the child. */
public record DeleteAccountabilitiesForPartyCommand(PartyId partyId) {}
