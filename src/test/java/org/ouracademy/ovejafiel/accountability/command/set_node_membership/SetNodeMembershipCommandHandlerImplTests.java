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
  void aNewcomerJoinsTheGroupAndTheChurchKnowsThemWithTheSameState() {
    isIn(church);
    isIn(group);

    var result =
        handler.handle(
            new SetNodeMembershipCommand(person, group, List.of(church), MEMBERSHIP_IN_PROGRESS));

    assertThat(result.joined()).isTrue();
    verify(events).joined(person, church, "MEMBERSHIP_IN_PROGRESS");
    verify(events).joined(person, group, "MEMBERSHIP_IN_PROGRESS");
    // Part of the group, and the state.
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Accountability>> saved = ArgumentCaptor.forClass(List.class);
    verify(accountabilities).saveAll(saved.capture());
    assertThat(saved.getValue()).hasSize(2);
    // The church only gets the state: a membership there would give the access of a member to
    // the whole church.
    verify(accountabilities).save(any(Accountability.class));
    verify(setMemberState, never()).handle(any());
  }

  @Test
  void someoneTheChurchAlreadyKnowsKeepsTheirStateThere() {
    isIn(church, holding(MEMBER));
    isIn(group);

    handler.handle(
        new SetNodeMembershipCommand(person, group, List.of(church), MEMBERSHIP_IN_PROGRESS));

    // Attendee of the youth group while a member of the church.
    verify(events, never()).joined(person, church, "MEMBERSHIP_IN_PROGRESS");
    verify(events).joined(person, group, "MEMBERSHIP_IN_PROGRESS");
    verify(accountabilities, never()).save(any(Accountability.class));
  }

  @Test
  void someoneAlreadyInTheNodeChangesState() {
    isIn(group, holding(IS_PART_OF), holding(VISITOR));

    var result = handler.handle(new SetNodeMembershipCommand(person, group, List.of(), MEMBER));

    assertThat(result.joined()).isFalse();
    verify(setMemberState).handle(new SetMemberStateCommand(person, group, MEMBER));
    verify(accountabilities, never()).saveAll(anyList());
  }

  @Test
  void whoeverHoldsAPositionCannotBeLessThanAMember() {
    isIn(group, holding(IS_PART_OF), holding(MEMBER), holdingPosition());

    assertThatThrownBy(
            () -> handler.handle(new SetNodeMembershipCommand(person, group, List.of(), VISITOR)))
        .isInstanceOf(DomainValidationException.class);
    verify(setMemberState, never()).handle(any());
  }

  @Test
  void aPositionHolderCanStayAMember() {
    isIn(group, holding(IS_PART_OF), holding(MEMBER), holdingPosition());

    handler.handle(new SetNodeMembershipCommand(person, group, List.of(), MEMBER));

    verify(setMemberState).handle(new SetMemberStateCommand(person, group, MEMBER));
  }

  @Test
  void onlyAProgressStateIsAccepted() {
    assertThatThrownBy(
            () ->
                handler.handle(new SetNodeMembershipCommand(person, group, List.of(), IS_PART_OF)))
        .isInstanceOf(DomainValidationException.class);
  }
}
