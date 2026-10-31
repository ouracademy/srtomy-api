package org.ouracademy.ovejafiel.whatsapp.command.request_setup_link;

import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionMode;

/** {@code organizationId} may be any node: the connection belongs to its church. */
public record RequestSetupLinkCommand(PartyId organizationId, ConnectionMode mode) {}
