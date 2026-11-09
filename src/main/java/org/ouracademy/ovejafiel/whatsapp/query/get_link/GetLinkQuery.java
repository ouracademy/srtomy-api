package org.ouracademy.ovejafiel.whatsapp.query.get_link;

import org.ouracademy.ovejafiel.party.domain.PartyId;

public record GetLinkQuery(String subject, PartyId organizationId) {}
