package org.ouracademy.ovejafiel.billing.query.check_capacity;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * Asks whether the church of {@code organizationId} may add one more of {@code resource}. Only the
 * free plan has a maximum.
 */
public record CheckCapacityQuery(PartyId organizationId, Resource resource) {

  public enum Resource {
    PEOPLE,
    LOCATIONS
  }
}
