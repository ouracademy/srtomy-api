package org.ouracademy.ovejafiel.accountability.command.set_member_state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.ouracademy.ovejafiel.accountability.domain.Accountability;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityRepository;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

class SetMemberStateCommandHandlerImplTests {

  AccountabilityRepository accountabilities = mock(AccountabilityRepository.class);
  AccountabilityTypeRepository types = mock(AccountabilityTypeRepository.class);
  EntityManager entityManager = mock(EntityManager.class);
  MemberStateEvents events = mock(MemberStateEvents.class);
  SetMemberStateCommandHandlerImpl handler =
      new SetMemberStateCommandHandlerImpl(accountabilities, types, entityManager, events);

  PartyId person = PartyId.generate();
  PartyId organization = PartyId.generate();

  Accountability holding(AccountabilityTypeId typeId) {
    var type = mock(AccountabilityType.class);
    when(type.getId()).thenReturn(typeId);
    var accountability = mock(Accountability.class);
    when(accountability.getType()).thenReturn(type);
    return accountability;
  }

  @Test
  void replacesTheCurrentStateWithTheNewOne() {
    var visitor = holding(VISITOR);
    var partOf = holding(IS_PART_OF);
    when(accountabilities.findByParentIdAndChildId(organization, person))
        .thenReturn(List.of(visitor, partOf));
    var type = mock(AccountabilityType.class);
    when(types.getReferenceById(MEMBERSHIP_IN_PROGRESS)).thenReturn(type);

    handler.handle(new SetMemberStateCommand(person, organization, MEMBERSHIP_IN_PROGRESS));

    verify(accountabilities).deleteAll(List.of(visitor));
    verify(events).changed(person, organization, "VISITOR", "MEMBERSHIP_IN_PROGRESS");
    var saved = ArgumentCaptor.forClass(Accountability.class);
    verify(accountabilities).save(saved.capture());
    assertThat(saved.getValue().getType()).isSameAs(type);
  }

  @Test
  void settingTheStateTheyAlreadyHaveChangesNothing() {
    var member = holding(MEMBER);
    when(accountabilities.findByParentIdAndChildId(organization, person))
        .thenReturn(List.of(member));

    handler.handle(new SetMemberStateCommand(person, organization, MEMBER));

    verify(accountabilities).deleteAll(List.of());
    verify(accountabilities, never()).save(any());
    verifyNoInteractions(events);
  }

  @Test
  void aPersonWithoutAStateJoinsWithIt() {
    var partOf = holding(IS_PART_OF);
    when(accountabilities.findByParentIdAndChildId(organization, person))
        .thenReturn(List.of(partOf));
    when(types.getReferenceById(VISITOR)).thenReturn(mock(AccountabilityType.class));

    handler.handle(new SetMemberStateCommand(person, organization, VISITOR));

    verify(events).joined(person, organization, "VISITOR");
  }

  @Test
  void aDuplicatedStateIsCleanedUp() {
    var first = holding(MEMBER);
    var second = holding(MEMBER);
    var visitor = holding(VISITOR);
    when(accountabilities.findByParentIdAndChildId(organization, person))
        .thenReturn(List.of(first, second, visitor));

    handler.handle(new SetMemberStateCommand(person, organization, MEMBER));

    var deleted = ArgumentCaptor.forClass(List.class);
    verify(accountabilities).deleteAll(deleted.capture());
    assertThat(deleted.getValue()).containsExactly(visitor);
    verify(accountabilities, never()).save(any());
  }

  @Test
  void onlyAProgressStateIsAccepted() {
    assertThatThrownBy(
            () -> handler.handle(new SetMemberStateCommand(person, organization, IS_PART_OF)))
        .isInstanceOf(DomainValidationException.class);
    verify(accountabilities, never()).save(any());
  }
}
