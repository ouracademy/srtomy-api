package org.ouracademy.srtomy.organization.command;

import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.repository.OrganizationRepository;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.stereotype.Component;

/** Create/edit steps shared by several organization command handlers. */
@Component
@RequiredArgsConstructor
public class OrganizationWriter {

  private final OrganizationRepository organizationRepository;

  /**
   * Saves a new organization under {@code parentId} ({@code null} for a church). Fails with {@link
   * NoSuchElementException} when the parent does not exist and with a {@code
   * DomainValidationException} when the type cannot hang from the parent's type. Callers must run
   * inside a transaction.
   */
  public Organization create(OrganizationInfo info, Organization.Type type, PartyId parentId) {
    var parent =
        parentId == null
            ? null
            : organizationRepository
                .findById(parentId)
                .orElseThrow(
                    () ->
                        new NoSuchElementException(
                            "Parent organization not found: " + parentId.value()));
    var organization = Organization.createUnder(info.name(), type, parent);
    fillInfo(info, organization);
    return organizationRepository.save(organization);
  }

  /** Overwrites the descriptive fields of an existing organization; empty if it does not exist. */
  public Optional<Organization> editInfo(PartyId id, OrganizationInfo info) {
    return organizationRepository
        .findById(id)
        .map(
            organization -> {
              organization.setName(info.name());
              fillInfo(info, organization);
              return organizationRepository.save(organization);
            });
  }

  /**
   * Overwrites the info and type of an existing group or ministry; empty if it does not exist or is
   * not a group or ministry (churches and locations are not edited this way).
   */
  public Optional<Organization> editChild(
      PartyId id, OrganizationInfo info, Organization.Type type) {
    return organizationRepository
        .findById(id)
        .filter(Organization::isGroupOrMinistry)
        .map(
            organization -> {
              organization.setName(info.name());
              organization.changeType(type);
              fillInfo(info, organization);
              return organizationRepository.save(organization);
            });
  }

  private static void fillInfo(OrganizationInfo info, Organization organization) {
    organization.setDescription(info.description());
    organization.setPhoneNumber(info.phoneNumber());
    organization.setAddress(info.address());
    organization.setSocialLinks(info.socialLinks());
    organization.setSchedule(info.schedule());
    if (info.memberVisibility() != null) {
      organization.setMemberVisibility(info.memberVisibility());
    }
    if (info.showOrgChart() != null) {
      organization.setShowOrgChart(info.showOrgChart());
    }
  }
}
