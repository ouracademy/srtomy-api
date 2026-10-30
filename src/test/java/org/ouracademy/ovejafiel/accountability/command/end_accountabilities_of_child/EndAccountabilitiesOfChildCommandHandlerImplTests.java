package org.ouracademy.ovejafiel.accountability.command.end_accountabilities_of_child;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBER;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBER_PROGRESS_STATES;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.accountability.command.MemberStateEvents;
import org.ouracademy.ovejafiel.accountability.domain.Accountability;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class EndAccountabilitiesOfChildCommandHandlerImplTests {

  @Mock AccountabilityRepository accountabilityRepository;
  @Mock MemberStateEvents memberStateEvents;

  @Test
  void handleClosesTheAccountabilitiesInsteadOfDeletingThem() {
    var handler =
        new EndAccountabilitiesOfChildCommandHandlerImpl(
            accountabilityRepository, memberStateEvents);
    var child = PartyId.generate();
    var parents = List.of(PartyId.generate(), PartyId.generate());
    when(accountabilityRepository.endByChildIdAndParentIdIn(
            eq(child), eq(parents), any(LocalDateTime.class)))
        .thenReturn(3);

    var result = handler.handle(new EndAccountabilitiesOfChildCommand(child, parents));

    assertThat(result.ended()).isEqualTo(3);
    verify(accountabilityRepository)
        .endByChildIdAndParentIdIn(eq(child), eq(parents), any(LocalDateTime.class));
  }

  @Test
  void handleWithoutParentsDoesNothing() {
    var handler =
        new EndAccountabilitiesOfChildCommandHandlerImpl(
            accountabilityRepository, memberStateEvents);

    var result =
        handler.handle(new EndAccountabilitiesOfChildCommand(PartyId.generate(), List.of()));

    assertThat(result.ended()).isZero();
    verifyNoInteractions(accountabilityRepository);
  }

  @Test
  void handleRecordsThatTheChildLeftEachNodeWhereTheyHadAState() {
    var handler =
        new EndAccountabilitiesOfChildCommandHandlerImpl(
            accountabilityRepository, memberStateEvents);
    var child = PartyId.generate();
    var group = PartyId.generate();
    var type = mock(AccountabilityType.class);
    when(type.getId()).thenReturn(MEMBER);
    var parent = mock(org.ouracademy.ovejafiel.party.domain.Party.class);
    when(parent.getId()).thenReturn(group);
    var state = mock(Accountability.class);
    when(state.getType()).thenReturn(type);
    when(state.getParent()).thenReturn(parent);
    when(accountabilityRepository.findByChildIdAndParentIdInAndTypeIdIn(
            child, List.of(group), List.of(MEMBER_PROGRESS_STATES)))
        .thenReturn(List.of(state));

    handler.handle(new EndAccountabilitiesOfChildCommand(child, List.of(group)));

    verify(memberStateEvents).left(child, group, "MEMBER");
  }
}
