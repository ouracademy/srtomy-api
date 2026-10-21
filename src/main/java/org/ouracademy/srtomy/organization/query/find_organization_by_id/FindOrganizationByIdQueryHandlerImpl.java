package org.ouracademy.srtomy.organization.query.find_organization_by_id;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;
import org.ouracademy.srtomy.organization.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindOrganizationByIdQueryHandlerImpl implements FindOrganizationByIdQueryHandler {

  private final OrganizationRepository organizationRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<FindOrganizationResult> handle(FindOrganizationByIdQuery query) {
    return organizationRepository.findById(query.id()).map(FindOrganizationResult::from);
  }
}
