package org.ouracademy.ovejafiel.directory.query.get_church_directory;

import java.util.Comparator;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.directory.query.ChurchAccess;
import org.ouracademy.ovejafiel.directory.query.DirectoryNodeResult;
import org.ouracademy.ovejafiel.directory.query.DirectoryReader;
import org.ouracademy.ovejafiel.directory.query.DirectoryReader.Audience;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetChurchDirectoryQueryHandlerImpl implements GetChurchDirectoryQueryHandler {

  private final ChurchAccess churchAccess;
  private final DirectoryReader reader;
  private final FindOrganizationSubtreeQueryHandler findOrganizationSubtreeQueryHandler;

  @Override
  @Transactional(readOnly = true)
  public GetChurchDirectoryResult handle(GetChurchDirectoryQuery query) {
    churchAccess.requireMember(query.subject(), query.churchId());
    var ids =
        findOrganizationSubtreeQueryHandler
            .handle(new FindOrganizationSubtreeQuery(query.churchId()))
            .ids();
    var nodes = reader.nodes(ids);
    var church =
        nodes.stream()
            .filter(node -> node.getId().equals(query.churchId()))
            .findFirst()
            .filter(node -> node.getType() == Organization.Type.CHURCH)
            .orElseThrow(() -> new NoSuchElementException("Church not found"));
    var leaders =
        reader.leadersByNode(nodes.stream().map(Organization::getId).toList(), Audience.CHURCH);
    return new GetChurchDirectoryResult(
        nodes.stream()
            .sorted(
                Comparator.comparing((Organization node) -> node != church)
                    .thenComparing(
                        Organization::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
            .map(
                node ->
                    DirectoryNodeResult.of(
                        node, leaders.getOrDefault(node.getId(), java.util.List.of())))
            .toList());
  }
}
