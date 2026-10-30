package org.ouracademy.ovejafiel.organization.query.find_organization_ancestors;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.organization.repository.OrganizationRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindOrganizationAncestorsQueryHandlerImpl implements FindOrganizationAncestorsQueryHandler {

  private final OrganizationRepository organizationRepository;

  @Override
  @Transactional(readOnly = true)
  public FindOrganizationAncestorsResult handle(FindOrganizationAncestorsQuery query) {
    return new FindOrganizationAncestorsResult(
        organizationRepository.findAncestorIds(query.organizationId().value()).stream()
            .map(PartyId::new)
            .toList());
  }
}
