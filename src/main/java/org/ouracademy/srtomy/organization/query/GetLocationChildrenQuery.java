package org.ouracademy.srtomy.organization.query;

import org.ouracademy.srtomy.party.domain.PartyId;

/** The members (IS_PART_OF) and leaders (LEADER) of a location child (group or ministry). */
public record GetLocationChildrenQuery(PartyId locationChildId) {}
