package org.ouracademy.srtomy.organization.query;

import java.time.LocalDateTime;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.domain.SocialLinks;
import org.ouracademy.srtomy.party.domain.PartyId;

/** An organization as exposed outside the module (never the JPA entity). */
public record FindOrganizationResult(
    PartyId id,
    String name,
    String description,
    String address,
    String phoneNumber,
    Organization.Type type,
    PartyId parentId,
    SocialLinks socialLinks,
    LocalDateTime createDate,
    LocalDateTime lastModifiedDate) {

  public static FindOrganizationResult from(Organization organization) {
    var parent = organization.getParent();
    return new FindOrganizationResult(
        organization.getId(),
        organization.getName(),
        organization.getDescription(),
        organization.getAddress(),
        organization.getPhoneNumber(),
        organization.getType(),
        parent == null ? null : parent.getId(),
        organization.getSocialLinks(),
        organization.getCreateDate(),
        organization.getLastModifiedDate());
  }
}
