package org.ouracademy.ovejafiel.directory.query.get_public_page;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.directory.query.DirectoryReader;
import org.ouracademy.ovejafiel.directory.query.DirectoryReader.Audience;
import org.ouracademy.ovejafiel.directory.query.get_public_page.GetPublicPageResult.OrgChartNode;
import org.ouracademy.ovejafiel.directory.query.get_public_page.GetPublicPageResult.PositionPeople;
import org.ouracademy.ovejafiel.directory.repository.DirectoryRepository;
import org.ouracademy.ovejafiel.organization.domain.MemberVisibility;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetPublicPageQueryHandlerImpl implements GetPublicPageQueryHandler {

  private final DirectoryRepository directoryRepository;
  private final DirectoryReader reader;
  private final FindOrganizationSubtreeQueryHandler findOrganizationSubtreeQueryHandler;

  @Override
  @Transactional(readOnly = true)
  public Optional<GetPublicPageResult> handle(GetPublicPageQuery query) {
    return directoryRepository
        .findBySlug(query.slug())
        .filter(node -> node.getMemberVisibility() == MemberVisibility.PUBLIC)
        .map(this::page);
  }

  private GetPublicPageResult page(Organization node) {
    var leaders =
        reader
            .leadersByNode(List.of(node.getId()), Audience.PUBLIC)
            .getOrDefault(node.getId(), List.of());
    var social = node.getSocialLinks();
    if (node.isGroupOrMinistry()) {
      return new GetPublicPageResult(
          node.getName(),
          node.getType(),
          node.getDescription(),
          node.getSchedule(),
          null,
          null,
          social,
          leaders,
          reader.membersOf(node.getId(), Audience.PUBLIC),
          List.of(),
          null);
    }
    return new GetPublicPageResult(
        node.getName(),
        node.getType(),
        node.getDescription(),
        node.getSchedule(),
        node.getAddress(),
        node.getPhoneNumber(),
        social,
        leaders,
        null,
        reader.positionsOf(node.getId(), Audience.PUBLIC).entrySet().stream()
            .map(entry -> new PositionPeople(entry.getKey(), entry.getValue()))
            .toList(),
        node.isShowOrgChart() ? orgChart(node) : null);
  }

  /** The tree below the node, without the nodes that only leaders can see (nor their subtrees). */
  private OrgChartNode orgChart(Organization root) {
    var ids =
        findOrganizationSubtreeQueryHandler
            .handle(new FindOrganizationSubtreeQuery(root.getId()))
            .ids();
    var nodes = reader.nodes(ids);
    var leaders = reader.leadersByNode(ids, Audience.PUBLIC);
    var childrenByParent =
        nodes.stream()
            .filter(node -> node.getParent() != null)
            .filter(node -> node.getMemberVisibility() != MemberVisibility.LEADERS)
            .collect(Collectors.groupingBy(node -> node.getParent().getId()));
    return chart(root, childrenByParent, leaders);
  }

  private OrgChartNode chart(
      Organization node,
      Map<PartyId, List<Organization>> childrenByParent,
      Map<PartyId, List<String>> leaders) {
    Collection<Organization> children = childrenByParent.getOrDefault(node.getId(), List.of());
    return new OrgChartNode(
        node.getName(),
        node.getType(),
        node.getMemberVisibility() == MemberVisibility.PUBLIC ? node.getSlug() : null,
        leaders.getOrDefault(node.getId(), List.of()),
        children.stream()
            .sorted(
                java.util.Comparator.comparing(
                    Organization::getName,
                    java.util.Comparator.nullsLast(String::compareToIgnoreCase)))
            .map(child -> chart(child, childrenByParent, leaders))
            .toList());
  }
}
