package org.ouracademy.ovejafiel.attendance.command.mark_user_present;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.attendance.command.mark_person_present.MarkPersonPresentCommand;
import org.ouracademy.ovejafiel.attendance.command.mark_person_present.MarkPersonPresentCommandHandler;
import org.ouracademy.ovejafiel.attendance.command.mark_person_present.MarkPersonPresentResult;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.query.find_event_organization.EventOrganizationResult;
import org.ouracademy.ovejafiel.event.query.find_event_organization.FindEventOrganizationQuery;
import org.ouracademy.ovejafiel.event.query.find_event_organization.FindEventOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.query.find_linked_person.FindLinkedPersonQuery;
import org.ouracademy.ovejafiel.user.query.find_linked_person.FindLinkedPersonQueryHandler;
import org.ouracademy.ovejafiel.user.query.find_linked_person.FindLinkedPersonResult;

class MarkUserPresentCommandHandlerImplTests {

  FindEventOrganizationQueryHandler eventOrganization =
      mock(FindEventOrganizationQueryHandler.class);
  FindChurchOfOrganizationQueryHandler churches = mock(FindChurchOfOrganizationQueryHandler.class);
  FindLinkedPersonQueryHandler linkedPeople = mock(FindLinkedPersonQueryHandler.class);
  MarkPersonPresentCommandHandler markPerson = mock(MarkPersonPresentCommandHandler.class);
  MarkUserPresentCommandHandlerImpl handler =
      new MarkUserPresentCommandHandlerImpl(eventOrganization, churches, linkedPeople, markPerson);

  EventId eventId = EventId.generate();
  UserId userId = UserId.generate();
  PartyId organizationId = PartyId.generate();
  PartyId churchId = PartyId.generate();

  void eventInChurch() {
    when(eventOrganization.handle(new FindEventOrganizationQuery(eventId)))
        .thenReturn(Optional.of(new EventOrganizationResult(organizationId)));
    when(churches.handle(new FindChurchOfOrganizationQuery(organizationId)))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(churchId)));
  }

  @Test
  void marksThePersonTheAccountIsInTheChurchOfTheEvent() {
    eventInChurch();
    var personId = PartyId.generate();
    when(linkedPeople.handle(new FindLinkedPersonQuery(userId, churchId)))
        .thenReturn(Optional.of(new FindLinkedPersonResult(personId)));
    var marker = UserId.generate();
    var command = new MarkPersonPresentCommand(eventId, personId, CheckInSource.QR_TICKET, marker);
    when(markPerson.handle(command)).thenReturn(new MarkPersonPresentResult(true));

    var result =
        handler.handle(
            new MarkUserPresentCommand(eventId, userId, CheckInSource.QR_TICKET, marker));

    assertThat(result).contains(new MarkUserPresentResult(personId, true));
  }

  @Test
  void anAccountWithoutAPersonInThatChurchMarksNobody() {
    eventInChurch();
    when(linkedPeople.handle(new FindLinkedPersonQuery(userId, churchId)))
        .thenReturn(Optional.empty());

    var result =
        handler.handle(new MarkUserPresentCommand(eventId, userId, CheckInSource.QR_EVENT, null));

    assertThat(result).isEmpty();
    verify(markPerson, never()).handle(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void anUnknownEventIsNotFound() {
    when(eventOrganization.handle(new FindEventOrganizationQuery(eventId)))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                handler.handle(
                    new MarkUserPresentCommand(eventId, userId, CheckInSource.QR_EVENT, null)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
