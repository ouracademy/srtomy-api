package org.ouracademy.ovejafiel.organization.query.get_location_children;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** The members (IS_PART_OF) and leaders (LEADER) of a location child (group or ministry). */
public record GetLocationChildrenQuery(PartyId locationChildId) {}
