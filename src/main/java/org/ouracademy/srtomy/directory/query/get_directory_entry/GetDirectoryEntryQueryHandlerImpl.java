package org.ouracademy.srtomy.directory.query.get_directory_entry;

import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.directory.query.ChurchAccess;
import org.ouracademy.srtomy.directory.query.DirectoryNodeResult;
import org.ouracademy.srtomy.directory.query.DirectoryReader;
import org.ouracademy.srtomy.directory.query.DirectoryReader.Audience;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.organization.domain.MemberVisibility;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetDirectoryEntryQueryHandlerImpl implements GetDirectoryEntryQueryHandler {

  private final ChurchAccess churchAccess;
  private final DirectoryReader reader;
  private final FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;

  @Override
  @Transactional(readOnly = true)
  public GetDirectoryEntryResult handle(GetDirectoryEntryQuery query) {
    var churchId =
        findChurchOfOrganizationQueryHandler
            .handle(new FindChurchOfOrganizationQuery(query.nodeId()))
            .orElseThrow(() -> new NoSuchElementException("Organization not found"))
            .churchId();
    churchAccess.requireMember(query.subject(), churchId);

    var node =
        reader.nodes(List.of(query.nodeId())).stream()
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Organization not found"));
    var children = childrenOf(node);
    var ids = new java.util.ArrayList<PartyId>();
    ids.add(node.getId());
    children.forEach(child -> ids.add(child.getId()));
    var leaders = reader.leadersByNode(ids, Audience.CHURCH);

    var visible = membersVisible(query.subject(), churchId, node);
    return new GetDirectoryEntryResult(
        DirectoryNodeResult.of(node, leaders.getOrDefault(node.getId(), List.of())),
        visible,
        visible ? reader.membersOf(node.getId(), Audience.CHURCH) : List.of(),
        children.stream()
            .map(
                child ->
                    DirectoryNodeResult.of(child, leaders.getOrDefault(child.getId(), List.of())))
            .toList());
  }

  private List<Organization> childrenOf(Organization node) {
    return reader.childrenOf(node.getId());
  }

  private boolean membersVisible(String subject, PartyId churchId, Organization node) {
    var visibility = node.getMemberVisibility();
    if (visibility == MemberVisibility.PUBLIC || visibility == MemberVisibility.CHURCH) {
      return true;
    }
    if (churchAccess.can(subject, node.getId(), Permission.MEMBERS_READ)) {
      return true;
    }
    return visibility == MemberVisibility.MEMBERS
        && churchAccess.isMemberOf(subject, churchId, node.getId());
  }
}
