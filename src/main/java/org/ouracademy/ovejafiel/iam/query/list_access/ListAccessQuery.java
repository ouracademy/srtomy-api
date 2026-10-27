package org.ouracademy.ovejafiel.iam.query.list_access;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** The role bindings that apply to a node: the ones made on it and the ones inherited. */
public record ListAccessQuery(PartyId organizationId) {}
