package org.ouracademy.ovejafiel.directory.query.get_my_nodes;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.LEADER;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.query.PositionResult;
import org.ouracademy.ovejafiel.accountability.query.get_person_accountabilities.GetPersonAccountabilitiesQuery;
import org.ouracademy.ovejafiel.accountability.query.get_person_accountabilities.GetPersonAccountabilitiesQueryHandler;
import org.ouracademy.ovejafiel.accountability.query.get_person_accountabilities.PersonAccountabilityResult;
import org.ouracademy.ovejafiel.iam.query.find_accessible_organizations.AccessibleOrganizationResult;
import org.ouracademy.ovejafiel.iam.query.find_accessible_organizations.FindAccessibleOrganizationsQuery;
import org.ouracademy.ovejafiel.iam.query.find_accessible_organizations.FindAccessibleOrganizationsQueryHandler;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.query.find_linked_person.FindLinkedPersonQuery;
import org.ouracademy.ovejafiel.user.query.find_linked_person.FindLinkedPersonQueryHandler;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQuery;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Only ever reads the accountabilities of the person linked to the caller, so nothing about other
 * people (or minors) can leak through it.
 */
@Service
@RequiredArgsConstructor
class GetMyNodesQueryHandlerImpl implements GetMyNodesQueryHandler {

  private final FindUserBySubjectQueryHandler findUserBySubjectQueryHandler;
  private final FindAccessibleOrganizationsQueryHandler findAccessibleOrganizationsQueryHandler;
  private final FindLinkedPersonQueryHandler findLinkedPersonQueryHandler;
  private final GetPersonAccountabilitiesQueryHandler getPersonAccountabilitiesQueryHandler;

  @Override
  @Transactional(readOnly = true)
  public List<MyNodeResult> handle(GetMyNodesQuery query) {
    var user = findUserBySubjectQueryHandler.handle(new FindUserBySubjectQuery(query.subject()));
    if (user.isEmpty()) {
      return List.of();
    }
    var tree =
        findAccessibleOrganizationsQueryHandler.handle(
            new FindAccessibleOrganizationsQuery(query.subject()));
    var byId = new LinkedHashMap<PartyId, AccessibleOrganizationResult>();
    tree.forEach(node -> byId.put(node.id(), node));

    return tree.stream()
        .filter(church -> church.type() == Organization.Type.CHURCH)
        .flatMap(
            church ->
                findLinkedPersonQueryHandler
                    .handle(new FindLinkedPersonQuery(user.get().id(), church.id()))
                    .stream()
                    .flatMap(linked -> nodesOf(linked.personId(), church, byId).stream()))
        .sorted(
            Comparator.comparing(MyNodeResult::churchName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(MyNodeResult::name, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  private List<MyNodeResult> nodesOf(
      PartyId personId,
      AccessibleOrganizationResult church,
      Map<PartyId, AccessibleOrganizationResult> byId) {
    var linksByNode =
        getPersonAccountabilitiesQueryHandler
            .handle(new GetPersonAccountabilitiesQuery(personId))
            .stream()
            .filter(link -> byId.containsKey(link.organizationId()))
            .filter(link -> isIn(church, link.organizationId(), byId))
            .filter(GetMyNodesQueryHandlerImpl::isNodeLink)
            .collect(
                groupingBy(
                    PersonAccountabilityResult::organizationId, LinkedHashMap::new, toList()));
    return linksByNode.entrySet().stream()
        .map(
            entry -> {
              var node = byId.get(entry.getKey());
              var links = entry.getValue();
              return new MyNodeResult(
                  node.id(),
                  node.name(),
                  node.type(),
                  church.id(),
                  church.name(),
                  links.stream().anyMatch(link -> link.typeId().equals(LEADER)),
                  links.stream()
                      .map(PersonAccountabilityResult::position)
                      .filter(Objects::nonNull)
                      .map(PositionResult::name)
                      .sorted()
                      .toList());
            })
        .toList();
  }

  /** Leading, belonging or holding a position; other system accountabilities are not links. */
  private static boolean isNodeLink(PersonAccountabilityResult link) {
    return link.position() != null
        || link.typeId().equals(LEADER)
        || link.typeId().equals(IS_PART_OF);
  }

  /** Whether {@code nodeId} is the church or hangs from it, walking up the visible tree. */
  private static boolean isIn(
      AccessibleOrganizationResult church,
      PartyId nodeId,
      Map<PartyId, AccessibleOrganizationResult> byId) {
    var current = byId.get(nodeId);
    while (current != null) {
      if (current.id().equals(church.id())) {
        return true;
      }
      current = current.parentId() == null ? null : byId.get(current.parentId());
    }
    return false;
  }
}
