package org.ouracademy.srtomy.organization;

import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.test.util.ReflectionTestUtils;

/** Test helpers for the organization module's tests. */
public final class OrganizationFixtures {

  private OrganizationFixtures() {}

  public static Organization organization(PartyId id, String name, Organization.Type type) {
    var organization = new Organization(name, type);
    ReflectionTestUtils.setField(organization, "id", id);
    return organization;
  }
}
