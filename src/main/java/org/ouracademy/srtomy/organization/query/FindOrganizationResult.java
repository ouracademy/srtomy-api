package org.ouracademy.srtomy.organization.query;

import java.time.LocalDateTime;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;

/** An organization as exposed outside the module (never the JPA entity). */
public record FindOrganizationResult(
    PartyId id,
    String name,
    String description,
    String address,
    String phoneNumber,
    Organization.Type type,
    LocalDateTime createDate,
    LocalDateTime lastModifiedDate) {

  public static FindOrganizationResult from(Organization organization) {
    return new FindOrganizationResult(
        organization.getId(),
        organization.getName(),
        organization.getDescription(),
        organization.getAddress(),
        organization.getPhoneNumber(),
        organization.getType(),
        organization.getCreateDate(),
        organization.getLastModifiedDate());
  }
}
