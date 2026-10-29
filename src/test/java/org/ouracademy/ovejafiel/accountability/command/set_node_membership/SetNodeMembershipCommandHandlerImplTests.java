package org.ouracademy.ovejafiel.accountability.command.set_node_membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.LEADER;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBER;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBERSHIP_IN_PROGRESS;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.VISITOR;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.ovejafiel.accountability.command.MemberStateEvents;
import org.ouracademy.ovejafiel.accountability.command.set_member_state.SetMemberStateCommand;
import org.ouracademy.ovejafiel.accountability.command.set_member_state.SetMemberStateCommandHandler;
import org.ouracademy.ovejafiel.accountability.domain.Accountability;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityRepository;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

class SetNodeMembershipCommandHandlerImplTests {

  AccountabilityRepository accountabilities = mock(AccountabilityRepository.class);
  AccountabilityTypeRepository types = mock(AccountabilityTypeRepository.class);
  SetMemberStateCommandHandler setMemberState = mock(SetMemberStateCommandHandler.class);
  MemberStateEvents events = mock(MemberStateEvents.class);
  EntityManager entityManager = mock(EntityManager.class);
  SetNodeMembershipCommandHandlerImpl handler =
      new SetNodeMembershipCommandHandlerImpl(
          accountabilities, types, setMemberState, events, entityManager);

  PartyId person = PartyId.generate();
  PartyId church = PartyId.generate();
  PartyId group = PartyId.generate();

  Accountability holding(AccountabilityTypeId typeId) {
    var type = mock(AccountabilityType.class);
    when(type.getId()).thenReturn(typeId);
    var accountability = mock(Accountability.class);
    when(accountability.getType()).thenReturn(type);
    return accountability;
  }

  Accountability holdingPosition() {
    var accountability = holding(AccountabilityTypeId.generate());
    when(accountability.getType().isPosition()).thenReturn(true);
    return accountability;
  }

  void isIn(PartyId node, Accountability... held) {
    when(accountabilities.findByParentIdAndChildId(node, person)).thenReturn(List.of(held));
  }

  @Test
  void aNewcomerJoinsTheGroupWithTheStateAsked() {
    isIn(group);

    var result =
        handler.handle(new SetNodeMembershipCommand(person, group, MEMBERSHIP_IN_PROGRESS, false));

    assertThat(result.joined()).isTrue();
    verify(events).joined(person, group, "MEMBERSHIP_IN_PROGRESS");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Accountability>> saved = ArgumentCaptor.forClass(List.class);
    verify(accountabilities).saveAll(saved.capture());
    // Part of the group, and the state.
    assertThat(saved.getValue()).hasSize(2);
    verify(setMemberState, never()).handle(any());
  }

  @Test
  void theChurchIsNotTouchedWhenSomeoneJoinsAGroup() {
    isIn(group);

    handler.handle(new SetNodeMembershipCommand(person, group, MEMBER, false));

    // Joining a group would otherwise make them a member (with its access) of the whole church.
    verify(accountabilities, org.mockito.Mockito.atLeastOnce())
        .findByParentIdAndChildId(group, person);
    verify(accountabilities, never()).findByParentIdAndChildId(church, person);
  }

  @Test
  void someoneAlreadyInTheNodeChangesState() {
    isIn(group, holding(IS_PART_OF), holding(VISITOR));

    var result = handler.handle(new SetNodeMembershipCommand(person, group, MEMBER, false));

    assertThat(result.joined()).isFalse();
    verify(setMemberState).handle(new SetMemberStateCommand(person, group, MEMBER));
    verify(accountabilities, never()).saveAll(anyList());
  }

  @Test
  void leadingAddsTheLeaderAccountabilityOnce() {
    isIn(group, holding(IS_PART_OF), holding(MEMBER));
    when(types.getReferenceById(LEADER)).thenReturn(mock(AccountabilityType.class));

    handler.handle(new SetNodeMembershipCommand(person, group, MEMBER, true));

    verify(accountabilities).save(any(Accountability.class));
  }

  @Test
  void anExistingLeaderIsNotAddedAgain() {
    isIn(group, holding(IS_PART_OF), holding(MEMBER), holding(LEADER));

    handler.handle(new SetNodeMembershipCommand(person, group, MEMBER, true));

    verify(accountabilities, never()).save(any(Accountability.class));
  }

  @Test
  void stoppingLeadingRemovesTheLeaderAccountabilityOnly() {
    var leads = holding(LEADER);
    isIn(group, holding(IS_PART_OF), holding(MEMBER), leads);

    handler.handle(new SetNodeMembershipCommand(person, group, MEMBER, false));

    verify(accountabilities).deleteAll(List.of(leads));
  }

  @Test
  void whoeverHoldsAPositionCannotBeLessThanAMember() {
    isIn(group, holding(IS_PART_OF), holding(MEMBER), holdingPosition());

    assertThatThrownBy(
            () -> handler.handle(new SetNodeMembershipCommand(person, group, VISITOR, false)))
        .isInstanceOf(DomainValidationException.class);
    verify(setMemberState, never()).handle(any());
  }

  @Test
  void aPositionHolderCanStayAMember() {
    isIn(group, holding(IS_PART_OF), holding(MEMBER), holdingPosition());

    handler.handle(new SetNodeMembershipCommand(person, group, MEMBER, false));

    verify(setMemberState).handle(new SetMemberStateCommand(person, group, MEMBER));
  }

  @Test
  void onlyAProgressStateIsAccepted() {
    assertThatThrownBy(
            () -> handler.handle(new SetNodeMembershipCommand(person, group, IS_PART_OF, false)))
        .isInstanceOf(DomainValidationException.class);
  }
}
