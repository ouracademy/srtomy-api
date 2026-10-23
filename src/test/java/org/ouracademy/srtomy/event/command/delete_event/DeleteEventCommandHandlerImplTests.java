package org.ouracademy.srtomy.event.command.delete_event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.ouracademy.srtomy.attendance.command.delete_attendance_for_events.DeleteAttendanceForEventsCommand;
import org.ouracademy.srtomy.attendance.command.delete_attendance_for_events.DeleteAttendanceForEventsCommandHandler;
import org.ouracademy.srtomy.event.domain.EventChangeOption;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.domain.RecurrentEvent;
import org.ouracademy.srtomy.event.domain.SingleEvent;
import org.ouracademy.srtomy.event.repository.SingleEventRepository;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class DeleteEventCommandHandlerImplTests {

  @Mock SingleEventRepository singleEventRepository;
  @Mock DeleteAttendanceForEventsCommandHandler deleteAttendanceForEventsCommandHandler;

  DeleteEventCommandHandlerImpl handler;

  RecurrentEvent recurrent;
  SingleEvent event;

  @BeforeEach
  void setUp() {
    handler =
        new DeleteEventCommandHandlerImpl(
            singleEventRepository, deleteAttendanceForEventsCommandHandler);
    var church = organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH);
    recurrent = recurrentEvent(EventId.generate(), church);
    event = occurrenceOf(recurrent, EventId.generate());
  }

  private void givenTheEventExists() {
    when(singleEventRepository.findById(event.getId())).thenReturn(Optional.of(event));
  }

  @Test
  void thisEventSoftDeletesOnlyThatEventAndItsAttendance() {
    givenTheEventExists();

    var result =
        handler.handle(new DeleteEventCommand(event.getId(), EventChangeOption.THIS_EVENT));

    assertThat(result.id()).isEqualTo(event.getId());
    assertThat(event.getDeletedTime()).isNotNull();
    verify(deleteAttendanceForEventsCommandHandler)
        .handle(new DeleteAttendanceForEventsCommand(List.of(event.getId())));
  }

  @Test
  void thisAndTheFollowingEventsSoftDeletesTheEventAndTheLaterOccurrences() {
    givenTheEventExists();
    var following = occurrenceOf(recurrent, EventId.generate());
    when(singleEventRepository.findByRecurrentEventAndStartAfter(recurrent, event.getStart()))
        .thenReturn(List.of(following));

    handler.handle(
        new DeleteEventCommand(event.getId(), EventChangeOption.THIS_AND_THE_FOLLOWING_EVENTS));

    assertThat(following.getDeletedTime()).isNotNull();
    assertThat(event.getDeletedTime()).isNotNull();
    verify(deleteAttendanceForEventsCommandHandler)
        .handle(new DeleteAttendanceForEventsCommand(List.of(following.getId(), event.getId())));
  }

  @Test
  void allEventsSoftDeletesEveryOccurrenceAndTheRecurrentEvent() {
    givenTheEventExists();
    var other = occurrenceOf(recurrent, EventId.generate());
    when(singleEventRepository.findByRecurrentEvent(recurrent)).thenReturn(List.of(event, other));

    handler.handle(new DeleteEventCommand(event.getId(), EventChangeOption.ALL_EVENTS));

    assertThat(event.getDeletedTime()).isNotNull();
    assertThat(other.getDeletedTime()).isNotNull();
    assertThat(recurrent.getDeletedTime()).isNotNull();
    verify(deleteAttendanceForEventsCommandHandler)
        .handle(
            new DeleteAttendanceForEventsCommand(
                List.of(event.getId(), other.getId(), recurrent.getId())));
  }

  @Test
  void deletingAMissingEventThrows() {
    var missingId = EventId.generate();
    when(singleEventRepository.findById(missingId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> handler.handle(new DeleteEventCommand(missingId, EventChangeOption.THIS_EVENT)))
        .isInstanceOf(NoSuchElementException.class);
    verifyNoInteractions(deleteAttendanceForEventsCommandHandler);
  }
}
