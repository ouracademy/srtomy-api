package org.ouracademy.ovejafiel.organization.query.find_organization_subtree;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.organization.repository.OrganizationRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The ids of a node and all its descendants, used to list what lives below a node. */
@Service
@RequiredArgsConstructor
class FindOrganizationSubtreeQueryHandlerImpl implements FindOrganizationSubtreeQueryHandler {

  private final OrganizationRepository organizationRepository;

  @Override
  @Transactional(readOnly = true)
  public FindOrganizationSubtreeResult handle(FindOrganizationSubtreeQuery query) {
    return new FindOrganizationSubtreeResult(
        organizationRepository.findSubtreeIds(query.rootId().value()).stream()
            .map(PartyId::new)
            .toList());
  }
}
