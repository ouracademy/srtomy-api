package org.ouracademy.srtomy.organization.query.find_organization_children;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.query.get_accountabilities_by_parent_and_types.AccountabilityResult;
import org.ouracademy.srtomy.accountability.query.get_accountabilities_by_parent_and_types.GetAccountabilitiesByParentAndTypesQuery;
import org.ouracademy.srtomy.accountability.query.get_accountabilities_by_parent_and_types.GetAccountabilitiesByParentAndTypesQueryHandler;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;
import org.ouracademy.srtomy.organization.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Accountability only knows children as generic parties, so this module (which owns the concept "is
 * an organization") keeps the child ids that resolve to an {@link Organization}, preserving
 * accountability order.
 */
@Service
@RequiredArgsConstructor
class FindOrganizationChildrenQueryHandlerImpl implements FindOrganizationChildrenQueryHandler {

  private final GetAccountabilitiesByParentAndTypesQueryHandler
      getAccountabilitiesByParentAndTypesQueryHandler;
  private final OrganizationRepository organizationRepository;

  @Override
  @Transactional(readOnly = true)
  public List<FindOrganizationResult> handle(FindOrganizationChildrenQuery query) {
    var childIds =
        getAccountabilitiesByParentAndTypesQueryHandler
            .handle(
                new GetAccountabilitiesByParentAndTypesQuery(query.parentId(), List.of(IS_PART_OF)))
            .stream()
            .map(AccountabilityResult::childId)
            .toList();
    if (childIds.isEmpty()) {
      return List.of();
    }
    var organizationsById =
        organizationRepository.findAllById(childIds).stream()
            .collect(toMap(Organization::getId, identity()));
    return childIds.stream()
        .filter(organizationsById::containsKey)
        .map(id -> FindOrganizationResult.from(organizationsById.get(id)))
        .toList();
  }
}
