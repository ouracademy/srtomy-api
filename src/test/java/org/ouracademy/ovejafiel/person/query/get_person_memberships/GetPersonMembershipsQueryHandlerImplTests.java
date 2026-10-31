package org.ouracademy.ovejafiel.person.query.get_person_memberships;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBER;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBERSHIP_IN_PROGRESS;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.accountability.query.PositionResult;
import org.ouracademy.ovejafiel.accountability.query.get_person_accountabilities.GetPersonAccountabilitiesQueryHandler;
import org.ouracademy.ovejafiel.accountability.query.get_person_accountabilities.PersonAccountabilityResult;
import org.ouracademy.ovejafiel.organization.OrganizationFixtures;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.organization.query.FindOrganizationResult;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.MemberState;

class GetPersonMembershipsQueryHandlerImplTests {

  GetPersonAccountabilitiesQueryHandler accountabilities =
      mock(GetPersonAccountabilitiesQueryHandler.class);
  FindOrganizationByIdQueryHandler organizations = mock(FindOrganizationByIdQueryHandler.class);
  GetPersonMembershipsQueryHandlerImpl handler =
      new GetPersonMembershipsQueryHandlerImpl(accountabilities, organizations);

  PartyId person = PartyId.generate();

  void organization(PartyId id, String name, Organization.Type type) {
    when(organizations.handle(new FindOrganizationByIdQuery(id)))
        .thenReturn(
            Optional.of(
                FindOrganizationResult.from(OrganizationFixtures.organization(id, name, type))));
  }

  PersonAccountabilityResult of(PartyId node, AccountabilityTypeId type) {
    return new PersonAccountabilityResult(node, type, null);
  }

  @Test
  void groupsWhatThePersonHasByNodeAndSortsByName() {
    var church = PartyId.generate();
    var youth = PartyId.generate();
    organization(church, "Iglesia Central", Organization.Type.CHURCH);
    organization(youth, "Jóvenes", Organization.Type.GROUP);
    var treasurer =
        new PersonAccountabilityResult(
            youth,
            AccountabilityTypeId.generate(),
            new PositionResult(
                AccountabilityTypeId.generate(), "Tesorero", church, Set.of(), Set.of()));
    when(accountabilities.handle(any()))
        .thenReturn(
            List.of(
                of(youth, IS_PART_OF),
                of(youth, MEMBERSHIP_IN_PROGRESS),
                treasurer,
                of(church, IS_PART_OF),
                of(church, MEMBER)));

    var result = handler.handle(new GetPersonMembershipsQuery(person));

    assertThat(result)
        .extracting(PersonMembershipResult::organizationName)
        .containsExactly("Iglesia Central", "Jóvenes");
    var churchStanding = result.get(0);
    assertThat(churchStanding.state()).isEqualTo(MemberState.MEMBER);
    assertThat(churchStanding.positions()).isEmpty();
    var youthStanding = result.get(1);
    assertThat(youthStanding.state()).isEqualTo(MemberState.MEMBERSHIP_IN_PROGRESS);
    assertThat(youthStanding.positions()).containsExactly("Tesorero");
    assertThat(youthStanding.organizationType()).isEqualTo(Organization.Type.GROUP);
  }

  @Test
  void aNodeThatOnlyKnowsThePersonByTheirStateIsListed() {
    var node = PartyId.generate();
    organization(node, "Iglesia", Organization.Type.CHURCH);
    when(accountabilities.handle(any())).thenReturn(List.of(of(node, MEMBER)));

    var result = handler.handle(new GetPersonMembershipsQuery(person));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).state()).isEqualTo(MemberState.MEMBER);
  }

  @Test
  void aNodeWithOnlyAPositionIsLeftOut() {
    var node = PartyId.generate();
    organization(node, "Alabanza", Organization.Type.MINISTRY);
    when(accountabilities.handle(any()))
        .thenReturn(
            List.of(
                new PersonAccountabilityResult(
                    node,
                    AccountabilityTypeId.generate(),
                    new PositionResult(
                        AccountabilityTypeId.generate(),
                        "Tesorera",
                        PartyId.generate(),
                        Set.of(),
                        Set.of()))));

    assertThat(handler.handle(new GetPersonMembershipsQuery(person))).isEmpty();
  }
}
