package org.ouracademy.ovejafiel.organization.query;

import java.time.LocalDateTime;
import org.ouracademy.ovejafiel.organization.domain.MemberVisibility;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.organization.domain.SocialLinks;
import org.ouracademy.ovejafiel.party.domain.PartyId;

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
    String schedule,
    MemberVisibility memberVisibility,
    boolean showOrgChart,
    String slug,
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
        organization.getSchedule(),
        organization.getMemberVisibility(),
        organization.isShowOrgChart(),
        organization.getSlug(),
        organization.getCreateDate(),
        organization.getLastModifiedDate());
  }
}
