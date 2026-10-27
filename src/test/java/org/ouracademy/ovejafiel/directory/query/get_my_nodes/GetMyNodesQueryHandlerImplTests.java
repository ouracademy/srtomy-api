package org.ouracademy.ovejafiel.directory.query.get_my_nodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.LEADER;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBER;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.accountability.query.PositionResult;
import org.ouracademy.ovejafiel.accountability.query.get_person_accountabilities.GetPersonAccountabilitiesQueryHandler;
import org.ouracademy.ovejafiel.accountability.query.get_person_accountabilities.PersonAccountabilityResult;
import org.ouracademy.ovejafiel.iam.query.find_accessible_organizations.AccessibleOrganizationResult;
import org.ouracademy.ovejafiel.iam.query.find_accessible_organizations.FindAccessibleOrganizationsQueryHandler;
import org.ouracademy.ovejafiel.organization.domain.Organization.Type;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.query.UserResult;
import org.ouracademy.ovejafiel.user.query.find_linked_person.FindLinkedPersonQueryHandler;
import org.ouracademy.ovejafiel.user.query.find_linked_person.FindLinkedPersonResult;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;

class GetMyNodesQueryHandlerImplTests {

  FindUserBySubjectQueryHandler users = mock(FindUserBySubjectQueryHandler.class);
  FindAccessibleOrganizationsQueryHandler tree =
      mock(FindAccessibleOrganizationsQueryHandler.class);
  FindLinkedPersonQueryHandler linked = mock(FindLinkedPersonQueryHandler.class);
  GetPersonAccountabilitiesQueryHandler accountabilities =
      mock(GetPersonAccountabilitiesQueryHandler.class);
  GetMyNodesQueryHandlerImpl handler =
      new GetMyNodesQueryHandlerImpl(users, tree, linked, accountabilities);

  PartyId church = PartyId.generate();
  PartyId location = PartyId.generate();
  PartyId youth = PartyId.generate();
  PartyId praise = PartyId.generate();
  PartyId person = PartyId.generate();

  void signedIn() {
    when(users.handle(any()))
        .thenReturn(Optional.of(new UserResult(UserId.generate(), "me", "me@x.org", true)));
    when(tree.handle(any()))
        .thenReturn(
            List.of(
                node(church, "Iglesia", Type.CHURCH, null),
                node(location, "Sede", Type.LOCATION, church),
                node(youth, "Jóvenes", Type.GROUP, location),
                node(praise, "Alabanza", Type.MINISTRY, location)));
    when(linked.handle(any())).thenReturn(Optional.of(new FindLinkedPersonResult(person)));
  }

  static AccessibleOrganizationResult node(PartyId id, String name, Type type, PartyId parent) {
    return new AccessibleOrganizationResult(id, name, type, parent, true, List.of());
  }

  static PersonAccountabilityResult of(PartyId node, AccountabilityTypeId type) {
    return new PersonAccountabilityResult(node, type, null);
  }

  @Test
  void listsTheNodesWhereThePersonIsAMemberOrALeaderSortedByName() {
    signedIn();
    when(accountabilities.handle(any()))
        .thenReturn(List.of(of(youth, IS_PART_OF), of(praise, LEADER), of(praise, IS_PART_OF)));

    var nodes = handler.handle(new GetMyNodesQuery("me"));

    assertThat(nodes).extracting(MyNodeResult::name).containsExactly("Alabanza", "Jóvenes");
    assertThat(nodes).extracting(MyNodeResult::leader).containsExactly(true, false);
    assertThat(nodes).extracting(MyNodeResult::churchName).containsOnly("Iglesia");
  }

  @Test
  void addsThePositionNamesAndIgnoresOtherSystemAccountabilities() {
    signedIn();
    var position =
        new PositionResult(
            new AccountabilityTypeId(java.util.UUID.randomUUID()),
            "Tesorero",
            church,
            Set.of(),
            Set.of());
    when(accountabilities.handle(any()))
        .thenReturn(
            List.of(
                of(youth, IS_PART_OF),
                new PersonAccountabilityResult(youth, position.id(), position),
                of(location, MEMBER)));

    var nodes = handler.handle(new GetMyNodesQuery("me"));

    assertThat(nodes).hasSize(1);
    assertThat(nodes.get(0).positions()).containsExactly("Tesorero");
    assertThat(nodes.get(0).leader()).isFalse();
  }

  @Test
  void skipsNodesThatAreNotInTheUsersTree() {
    signedIn();
    when(accountabilities.handle(any())).thenReturn(List.of(of(PartyId.generate(), IS_PART_OF)));

    assertThat(handler.handle(new GetMyNodesQuery("me"))).isEmpty();
  }

  @Test
  void isEmptyWithoutALinkedPersonOrAnUnknownUser() {
    signedIn();
    when(linked.handle(any())).thenReturn(Optional.empty());
    assertThat(handler.handle(new GetMyNodesQuery("me"))).isEmpty();

    when(users.handle(any())).thenReturn(Optional.empty());
    assertThat(handler.handle(new GetMyNodesQuery("nobody"))).isEmpty();
  }
}
