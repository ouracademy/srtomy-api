package org.ouracademy.ovejafiel.whatsapp.command.disconnect;

import org.ouracademy.ovejafiel.party.domain.PartyId;

public record DisconnectCommand(PartyId organizationId) {}
