package org.ouracademy.srtomy.directory.query.get_church_directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.directory.query.ChurchAccess;
import org.ouracademy.srtomy.directory.query.DirectoryNodeResult;
import org.ouracademy.srtomy.directory.query.DirectoryReader;
import org.ouracademy.srtomy.directory.query.DirectoryReader.Audience;
import org.ouracademy.srtomy.organization.domain.MemberVisibility;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.ouracademy.srtomy.organization.query.find_organization_subtree.FindOrganizationSubtreeResult;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.security.access.AccessDeniedException;

class GetChurchDirectoryQueryHandlerImplTests {

  ChurchAccess churchAccess = mock(ChurchAccess.class);
  DirectoryReader reader = mock(DirectoryReader.class);
  FindOrganizationSubtreeQueryHandler subtree = mock(FindOrganizationSubtreeQueryHandler.class);
  GetChurchDirectoryQueryHandlerImpl handler =
      new GetChurchDirectoryQueryHandlerImpl(churchAccess, reader, subtree);

  PartyId churchId = PartyId.generate();
  Organization church = organization(churchId, "Iglesia", Organization.Type.CHURCH);
  Organization location =
      organization(PartyId.generate(), "Sede Norte", Organization.Type.LOCATION, church);
  Organization group =
      organization(PartyId.generate(), "alabanza", Organization.Type.GROUP, location);

  void stub(List<Organization> nodes) {
    var ids = nodes.stream().map(Organization::getId).toList();
    when(subtree.handle(any())).thenReturn(new FindOrganizationSubtreeResult(ids));
    when(reader.nodes(ids)).thenReturn(nodes);
    when(reader.leadersByNode(ids, Audience.CHURCH))
        .thenReturn(Map.of(group.getId(), List.of("Ana Ruiz")));
  }

  @Test
  void listsEveryNodeWithItsLeadersChurchFirstThenByName() {
    group.setMemberVisibility(MemberVisibility.LEADERS);
    stub(List.of(group, location, church));

    var nodes = handler.handle(new GetChurchDirectoryQuery("me", churchId)).nodes();

    assertThat(nodes)
        .extracting(DirectoryNodeResult::name)
        .containsExactly("Iglesia", "alabanza", "Sede Norte");
    // The node that hides its people is still in the directory.
    assertThat(nodes.get(1).memberVisibility()).isEqualTo(MemberVisibility.LEADERS);
    assertThat(nodes.get(1).leaders()).containsExactly("Ana Ruiz");
    assertThat(nodes.get(2).parentId()).isEqualTo(churchId);
    verify(churchAccess).requireMember("me", churchId);
  }

  @Test
  void aNodeThatIsNotAChurchIsNotFound() {
    var ids = List.of(location.getId());
    when(subtree.handle(any())).thenReturn(new FindOrganizationSubtreeResult(ids));
    when(reader.nodes(ids)).thenReturn(List.of(location));

    assertThatThrownBy(() -> handler.handle(new GetChurchDirectoryQuery("me", location.getId())))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void someoneOutsideTheChurchIsDeniedBeforeReadingAnything() {
    doThrow(new AccessDeniedException("no")).when(churchAccess).requireMember("me", churchId);

    assertThatThrownBy(() -> handler.handle(new GetChurchDirectoryQuery("me", churchId)))
        .isInstanceOf(AccessDeniedException.class);
    verify(reader, never()).nodes(any());
  }
}
