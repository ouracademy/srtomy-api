package org.ouracademy.srtomy.directory.query.get_my_nodes;

import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.LEADER;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.query.get_person_accountabilities.GetPersonAccountabilitiesQuery;
import org.ouracademy.srtomy.accountability.query.get_person_accountabilities.GetPersonAccountabilitiesQueryHandler;
import org.ouracademy.srtomy.iam.query.find_accessible_organizations.AccessibleOrganizationResult;
import org.ouracademy.srtomy.iam.query.find_accessible_organizations.FindAccessibleOrganizationsQuery;
import org.ouracademy.srtomy.iam.query.find_accessible_organizations.FindAccessibleOrganizationsQueryHandler;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.user.query.find_linked_person.FindLinkedPersonQuery;
import org.ouracademy.srtomy.user.query.find_linked_person.FindLinkedPersonQueryHandler;
import org.ouracademy.srtomy.user.query.find_user_by_subject.FindUserBySubjectQuery;
import org.ouracademy.srtomy.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
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

    var mine = new ArrayList<MyNodeResult>();
    for (var church : tree) {
      if (church.type() != Organization.Type.CHURCH) {
        continue;
      }
      findLinkedPersonQueryHandler
          .handle(new FindLinkedPersonQuery(user.get().id(), church.id()))
          .ifPresent(linked -> mine.addAll(nodesOf(linked.personId(), church, byId)));
    }
    return mine.stream()
        .sorted(
            Comparator.comparing(MyNodeResult::churchName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(MyNodeResult::name, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  private List<MyNodeResult> nodesOf(
      PartyId personId,
      AccessibleOrganizationResult church,
      Map<PartyId, AccessibleOrganizationResult> byId) {
    var leader = new LinkedHashMap<PartyId, Boolean>();
    var positions = new LinkedHashMap<PartyId, List<String>>();
    for (var accountability :
        getPersonAccountabilitiesQueryHandler.handle(
            new GetPersonAccountabilitiesQuery(personId))) {
      var nodeId = accountability.organizationId();
      if (!byId.containsKey(nodeId) || !isIn(church, nodeId, byId)) {
        continue;
      }
      var isLeader = accountability.typeId().equals(LEADER);
      var isPosition = accountability.position() != null;
      if (!isLeader && !isPosition && !accountability.typeId().equals(IS_PART_OF)) {
        // Another system accountability (e.g. a membership state): not a link to the node.
        continue;
      }
      leader.merge(nodeId, isLeader, Boolean::logicalOr);
      var names = positions.computeIfAbsent(nodeId, id -> new ArrayList<>());
      if (isPosition) {
        names.add(accountability.position().name());
      }
    }
    return leader.entrySet().stream()
        .map(
            entry -> {
              var node = byId.get(entry.getKey());
              return new MyNodeResult(
                  node.id(),
                  node.name(),
                  node.type(),
                  church.id(),
                  church.name(),
                  entry.getValue(),
                  positions.get(entry.getKey()).stream().sorted().toList());
            })
        .toList();
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
