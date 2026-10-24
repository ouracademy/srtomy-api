package org.ouracademy.srtomy.iam.query.list_access;

import org.ouracademy.srtomy.party.domain.PartyId;

/** The role bindings that apply to a node: the ones made on it and the ones inherited. */
public record ListAccessQuery(PartyId organizationId) {}
