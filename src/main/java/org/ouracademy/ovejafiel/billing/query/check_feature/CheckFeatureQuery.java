package org.ouracademy.ovejafiel.billing.query.check_feature;

import org.ouracademy.ovejafiel.billing.domain.Feature;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/** Asks whether the plan of the church of {@code organizationId} includes {@code feature}. */
public record CheckFeatureQuery(PartyId organizationId, Feature feature) {}
