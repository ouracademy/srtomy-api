package org.ouracademy.ovejafiel.organization.query.find_church_of_organization;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.organization.repository.OrganizationRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindChurchOfOrganizationQueryHandlerImpl implements FindChurchOfOrganizationQueryHandler {

  private final OrganizationRepository organizationRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<FindChurchOfOrganizationResult> handle(FindChurchOfOrganizationQuery query) {
    return organizationRepository
        .findChurchIdOf(query.organizationId().value())
        .map(id -> new FindChurchOfOrganizationResult(new PartyId(id)));
  }
}
