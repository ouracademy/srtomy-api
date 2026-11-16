package org.ouracademy.ovejafiel.organization.query.count_locations;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.organization.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CountLocationsQueryHandlerImpl implements CountLocationsQueryHandler {

  private final OrganizationRepository organizationRepository;

  @Override
  @Transactional(readOnly = true)
  public CountLocationsResult handle(CountLocationsQuery query) {
    return new CountLocationsResult(
        organizationRepository.countByParentIdAndType(
            query.churchId(), Organization.Type.LOCATION));
  }
}
