package org.ouracademy.ovejafiel.organization;

import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.test.util.ReflectionTestUtils;

/** Test helpers for the organization module's tests. */
public final class OrganizationFixtures {

  private OrganizationFixtures() {}

  public static Organization organization(PartyId id, String name, Organization.Type type) {
    var organization = new Organization(name, type);
    ReflectionTestUtils.setField(organization, "id", id);
    return organization;
  }

  public static Organization organization(
      PartyId id, String name, Organization.Type type, Organization parent) {
    var organization = organization(id, name, type);
    ReflectionTestUtils.setField(organization, "parent", parent);
    return organization;
  }
}
