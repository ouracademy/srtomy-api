package org.ouracademy.ovejafiel.accountability.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.ovejafiel.accountability.domain.MemberStateEvent;
import org.ouracademy.ovejafiel.accountability.repository.MemberStateEventRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class MemberStateEventsTests {

  MemberStateEventRepository repository = mock(MemberStateEventRepository.class);
  MemberStateEvents events = new MemberStateEvents(repository);
  PartyId person = PartyId.generate();
  PartyId node = PartyId.generate();

  MemberStateEvent saved() {
    var captor = ArgumentCaptor.forClass(MemberStateEvent.class);
    verify(repository).save(captor.capture());
    return captor.getValue();
  }

  @Test
  void joiningHasNoPreviousState() {
    events.joined(person, node, "VISITOR");

    var event = saved();
    assertThat(event.getFromState()).isNull();
    assertThat(event.getToState()).isEqualTo("VISITOR");
    assertThat(event.getPersonId()).isEqualTo(person);
    assertThat(event.getOrganizationId()).isEqualTo(node);
  }

  @Test
  void aChangeKeepsBothStates() {
    events.changed(person, node, "VISITOR", "MEMBER");

    var event = saved();
    assertThat(event.getFromState()).isEqualTo("VISITOR");
    assertThat(event.getToState()).isEqualTo("MEMBER");
  }

  @Test
  void leavingHasNoNewState() {
    events.left(person, node, "MEMBER");

    var event = saved();
    assertThat(event.getFromState()).isEqualTo("MEMBER");
    assertThat(event.getToState()).isNull();
  }

  @Test
  void keepingTheSameStateIsNotAnEvent() {
    events.changed(person, node, "MEMBER", "MEMBER");

    verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
  }
}
