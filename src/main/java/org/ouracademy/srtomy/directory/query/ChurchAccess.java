package org.ouracademy.srtomy.directory.query;

import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.query.get_person_accountabilities.GetPersonAccountabilitiesQuery;
import org.ouracademy.srtomy.accountability.query.get_person_accountabilities.GetPersonAccountabilitiesQueryHandler;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.iam.query.find_accessible_organizations.FindAccessibleOrganizationsQuery;
import org.ouracademy.srtomy.iam.query.find_accessible_organizations.FindAccessibleOrganizationsQueryHandler;
import org.ouracademy.srtomy.iam.query.get_effective_permissions.GetEffectivePermissionsQuery;
import org.ouracademy.srtomy.iam.query.get_effective_permissions.GetEffectivePermissionsQueryHandler;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.user.query.find_linked_person.FindLinkedPersonQuery;
import org.ouracademy.srtomy.user.query.find_linked_person.FindLinkedPersonQueryHandler;
import org.ouracademy.srtomy.user.query.find_user_by_subject.FindUserBySubjectQuery;
import org.ouracademy.srtomy.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** What the signed-in user is to a church and to a node of it, for the directory rules. */
@Component
@RequiredArgsConstructor
public class ChurchAccess {

  private final FindAccessibleOrganizationsQueryHandler findAccessibleOrganizationsQueryHandler;
  private final GetEffectivePermissionsQueryHandler getEffectivePermissionsQueryHandler;
  private final FindUserBySubjectQueryHandler findUserBySubjectQueryHandler;
  private final FindLinkedPersonQueryHandler findLinkedPersonQueryHandler;
  private final GetPersonAccountabilitiesQueryHandler getPersonAccountabilitiesQueryHandler;

  /**
   * Fails with {@code AccessDeniedException} (403) unless the user has access somewhere in the
   * church: any node of it, even only a group, puts the church in the user's tree.
   */
  public void requireMember(String subject, PartyId churchId) {
    var inChurch =
        findAccessibleOrganizationsQueryHandler
            .handle(new FindAccessibleOrganizationsQuery(subject))
            .stream()
            .anyMatch(node -> node.id().equals(churchId));
    if (!inChurch) {
      throw new AccessDeniedException("No perteneces a esta iglesia");
    }
  }

  public boolean can(String subject, PartyId nodeId, Permission permission) {
    return getEffectivePermissionsQueryHandler
        .handle(new GetEffectivePermissionsQuery(subject, nodeId))
        .permissions()
        .contains(permission);
  }

  /** Whether the person who represents the user belongs to the node, in any way. */
  public boolean isMemberOf(String subject, PartyId churchId, PartyId nodeId) {
    return findUserBySubjectQueryHandler
        .handle(new FindUserBySubjectQuery(subject))
        .flatMap(
            user ->
                findLinkedPersonQueryHandler.handle(new FindLinkedPersonQuery(user.id(), churchId)))
        .map(
            linked ->
                getPersonAccountabilitiesQueryHandler
                    .handle(new GetPersonAccountabilitiesQuery(linked.personId()))
                    .stream()
                    .anyMatch(accountability -> accountability.organizationId().equals(nodeId)))
        .orElse(false);
  }
}
