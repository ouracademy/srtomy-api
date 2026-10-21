package org.ouracademy.srtomy.event.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.event.EventFixtures.occurrenceOf;
import static org.ouracademy.srtomy.event.EventFixtures.recurrentEvent;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.event.domain.EventChangeOption;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.domain.RecurrentEvent;
import org.ouracademy.srtomy.event.domain.SingleEvent;
import org.ouracademy.srtomy.event.repository.EventRepository;
import org.ouracademy.srtomy.event.repository.SingleEventRepository;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class DeleteEventCommandHandlerImplTests {

  @Mock EventRepository eventRepository;
  @Mock SingleEventRepository singleEventRepository;

  DeleteEventCommandHandlerImpl handler;

  RecurrentEvent recurrent;
  SingleEvent event;

  @BeforeEach
  void setUp() {
    handler = new DeleteEventCommandHandlerImpl(eventRepository, singleEventRepository);
    var church = organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH);
    recurrent = recurrentEvent(EventId.generate(), church);
    event = occurrenceOf(recurrent, EventId.generate());
  }

  private void givenTheEventExists() {
    when(singleEventRepository.findById(event.getId())).thenReturn(Optional.of(event));
  }

  @Test
  void thisEventDeletesOnlyThatEvent() {
    givenTheEventExists();
    var result =
        handler.handle(new DeleteEventCommand(event.getId(), EventChangeOption.THIS_EVENT));

    assertThat(result.id()).isEqualTo(event.getId());
    verify(eventRepository).delete(event);
    verifyNoMoreInteractions(eventRepository);
  }

  @Test
  void thisAndTheFollowingEventsDeletesTheEventAndTheLaterOccurrences() {
    givenTheEventExists();
    var following = List.of(occurrenceOf(recurrent, EventId.generate()));
    when(singleEventRepository.findByRecurrentEventAndStartAfter(recurrent, event.getStart()))
        .thenReturn(following);

    handler.handle(
        new DeleteEventCommand(event.getId(), EventChangeOption.THIS_AND_THE_FOLLOWING_EVENTS));

    verify(eventRepository).deleteAll(following);
    verify(eventRepository).delete(event);
    verifyNoMoreInteractions(eventRepository);
  }

  @Test
  void allEventsDeletesEveryOccurrenceAndTheRecurrentEvent() {
    givenTheEventExists();
    var occurrences = List.of(event, occurrenceOf(recurrent, EventId.generate()));
    when(singleEventRepository.findByRecurrentEvent(recurrent)).thenReturn(occurrences);

    handler.handle(new DeleteEventCommand(event.getId(), EventChangeOption.ALL_EVENTS));

    verify(eventRepository).deleteAll(occurrences);
    verify(eventRepository).delete(recurrent);
    verifyNoMoreInteractions(eventRepository);
  }

  @Test
  void deletingAMissingEventThrows() {
    var missingId = EventId.generate();
    when(singleEventRepository.findById(missingId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> handler.handle(new DeleteEventCommand(missingId, EventChangeOption.THIS_EVENT)))
        .isInstanceOf(NoSuchElementException.class);
    verifyNoInteractions(eventRepository);
  }
}
