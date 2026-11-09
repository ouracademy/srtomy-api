package org.ouracademy.ovejafiel.whatsapp.query.get_connection;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** {@code organizationId} may be a branch: it shows the connection of its church. */
public record GetConnectionQuery(PartyId organizationId) {}
