package org.ouracademy.srtomy.organization.command;

import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.command.create_accountability.CreateAccountabilityCommand;
import org.ouracademy.srtomy.accountability.command.create_accountability.CreateAccountabilityCommandHandler;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.repository.OrganizationRepository;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.stereotype.Component;

/** Create/edit steps shared by several organization command handlers. */
@Component
@RequiredArgsConstructor
public class OrganizationWriter {

  private final OrganizationRepository organizationRepository;
  private final CreateAccountabilityCommandHandler createAccountabilityCommandHandler;

  /**
   * Saves a new organization and, when {@code parentId} is given, links it to its parent with an
   * IS_PART_OF accountability. Callers must run inside a transaction.
   */
  public Organization create(OrganizationInfo info, Organization.Type type, PartyId parentId) {
    var organization = new Organization(info.name(), type);
    fillInfo(info, organization);
    organization = organizationRepository.save(organization);

    if (parentId != null) {
      createAccountabilityCommandHandler.handle(
          new CreateAccountabilityCommand(organization.getId(), parentId, IS_PART_OF));
    }

    return organization;
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
  }
}
