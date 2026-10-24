package org.ouracademy.srtomy.iam.query.find_accessible_organizations;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.iam.repository.RoleBindingRepository;
import org.ouracademy.srtomy.iam.repository.RoleBindingRepository.BoundRole;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindAccessibleOrganizationsQueryHandlerImpl
    implements FindAccessibleOrganizationsQueryHandler {

  private final RoleBindingRepository roleBindingRepository;

  @Override
  @Transactional(readOnly = true)
  public List<AccessibleOrganizationResult> handle(FindAccessibleOrganizationsQuery query) {
    var rolesByOrganization =
        roleBindingRepository.findBoundRoles(query.subject()).stream()
            .collect(
                groupingBy(
                    BoundRole::getOrganizationId, mapping(BoundRole::getRoleName, toList())));
    return roleBindingRepository.findVisibleOrganizations(query.subject()).stream()
        .map(
            node -> {
              var id = new PartyId(node.getId());
              return new AccessibleOrganizationResult(
                  id,
                  node.getName(),
                  Organization.Type.valueOf(node.getType()),
                  node.getParentId() == null ? null : new PartyId(node.getParentId()),
                  node.getAccessible(),
                  rolesByOrganization.getOrDefault(id, List.of()));
            })
        .toList();
  }
}
