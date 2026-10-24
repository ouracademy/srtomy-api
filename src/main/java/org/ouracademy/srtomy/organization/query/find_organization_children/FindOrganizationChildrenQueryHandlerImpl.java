package org.ouracademy.srtomy.organization.query.find_organization_children;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;
import org.ouracademy.srtomy.organization.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The direct child nodes of an organization in the tree, by name. */
@Service
@RequiredArgsConstructor
class FindOrganizationChildrenQueryHandlerImpl implements FindOrganizationChildrenQueryHandler {

  private final OrganizationRepository organizationRepository;

  @Override
  @Transactional(readOnly = true)
  public List<FindOrganizationResult> handle(FindOrganizationChildrenQuery query) {
    return organizationRepository.findByParentIdOrderByName(query.parentId()).stream()
        .map(FindOrganizationResult::from)
        .toList();
  }
}
