package org.ouracademy.srtomy.directory.query.get_directory_entry;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.directory.query.ChurchAccess;
import org.ouracademy.srtomy.directory.query.DirectoryReader;
import org.ouracademy.srtomy.directory.query.DirectoryReader.Audience;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.organization.domain.MemberVisibility;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.security.access.AccessDeniedException;

class GetDirectoryEntryQueryHandlerImplTests {

  ChurchAccess churchAccess = mock(ChurchAccess.class);
  DirectoryReader reader = mock(DirectoryReader.class);
  FindChurchOfOrganizationQueryHandler findChurch =
      mock(FindChurchOfOrganizationQueryHandler.class);
  GetDirectoryEntryQueryHandlerImpl handler =
      new GetDirectoryEntryQueryHandlerImpl(churchAccess, reader, findChurch);

  PartyId church = PartyId.generate();
  PartyId groupId = PartyId.generate();
  Organization group = organization(groupId, "Jóvenes", Organization.Type.GROUP);
  Organization child = organization(PartyId.generate(), "Sub", Organization.Type.GROUP, group);

  @BeforeEach
  void setUp() {
    group.setSchedule("Sábados");
    when(findChurch.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
    when(reader.nodes(List.of(groupId))).thenReturn(List.of(group));
    when(reader.childrenOf(groupId)).thenReturn(List.of(child));
    when(reader.leadersByNode(any(), any(Audience.class)))
        .thenReturn(Map.of(groupId, List.of("Ana Ruiz")));
    when(reader.membersOf(groupId, Audience.CHURCH)).thenReturn(List.of("Beto Paz"));
  }

  GetDirectoryEntryResult entry(MemberVisibility visibility) {
    group.setMemberVisibility(visibility);
    return handler.handle(new GetDirectoryEntryQuery("me", groupId));
  }

  @Test
  void theBasicInformationAndTheChildrenAreAlwaysThere() {
    var result = entry(MemberVisibility.LEADERS);

    assertThat(result.node().name()).isEqualTo("Jóvenes");
    assertThat(result.node().schedule()).isEqualTo("Sábados");
    assertThat(result.node().leaders()).containsExactly("Ana Ruiz");
    assertThat(result.children()).extracting(c -> c.name()).containsExactly("Sub");
    verify(churchAccess).requireMember("me", church);
  }

  @Test
  void publicAndChurchNodesShowTheirPeopleToAnyoneInTheChurch() {
    for (var visibility : List.of(MemberVisibility.PUBLIC, MemberVisibility.CHURCH)) {
      var result = entry(visibility);

      assertThat(result.membersVisible()).isTrue();
      assertThat(result.members()).containsExactly("Beto Paz");
    }
  }

  @Test
  void membersOnlyShowsThemToTheMembersAndToWhoCanReadPeople() {
    when(churchAccess.can("me", groupId, Permission.MEMBERS_READ)).thenReturn(false);
    when(churchAccess.isMemberOf("me", church, groupId)).thenReturn(false);
    assertThat(entry(MemberVisibility.MEMBERS).membersVisible()).isFalse();
    assertThat(entry(MemberVisibility.MEMBERS).members()).isEmpty();

    when(churchAccess.isMemberOf("me", church, groupId)).thenReturn(true);
    assertThat(entry(MemberVisibility.MEMBERS).membersVisible()).isTrue();

    when(churchAccess.isMemberOf("me", church, groupId)).thenReturn(false);
    when(churchAccess.can("me", groupId, Permission.MEMBERS_READ)).thenReturn(true);
    assertThat(entry(MemberVisibility.MEMBERS).membersVisible()).isTrue();
  }

  @Test
  void leadersOnlyShowsThemToWhoCanReadPeopleEvenIfTheyAreMembers() {
    when(churchAccess.isMemberOf("me", church, groupId)).thenReturn(true);
    when(churchAccess.can("me", groupId, Permission.MEMBERS_READ)).thenReturn(false);
    assertThat(entry(MemberVisibility.LEADERS).membersVisible()).isFalse();

    when(churchAccess.can("me", groupId, Permission.MEMBERS_READ)).thenReturn(true);
    var result = entry(MemberVisibility.LEADERS);
    assertThat(result.membersVisible()).isTrue();
    assertThat(result.members()).containsExactly("Beto Paz");
  }

  @Test
  void hiddenPeopleAreNotEvenRead() {
    entry(MemberVisibility.LEADERS);

    verify(reader, never()).membersOf(any(), any());
  }

  @Test
  void someoneOutsideTheChurchIsDenied() {
    doThrow(new AccessDeniedException("no")).when(churchAccess).requireMember("me", church);

    assertThatThrownBy(() -> entry(MemberVisibility.PUBLIC))
        .isInstanceOf(AccessDeniedException.class);
    verify(reader, never()).nodes(any());
  }

  @Test
  void anUnknownNodeIsNotFound() {
    when(findChurch.handle(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(new GetDirectoryEntryQuery("me", groupId)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
