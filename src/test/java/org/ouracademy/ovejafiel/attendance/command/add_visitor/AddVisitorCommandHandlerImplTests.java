package org.ouracademy.ovejafiel.attendance.command.add_visitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.ovejafiel.attendance.command.mark_person_present.MarkPersonPresentCommand;
import org.ouracademy.ovejafiel.attendance.command.mark_person_present.MarkPersonPresentCommandHandler;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.query.find_event_organization.EventOrganizationResult;
import org.ouracademy.ovejafiel.event.query.find_event_organization.FindEventOrganizationQuery;
import org.ouracademy.ovejafiel.event.query.find_event_organization.FindEventOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.command.PersonInfo;
import org.ouracademy.ovejafiel.person.command.create_person.CreatePersonCommand;
import org.ouracademy.ovejafiel.person.command.create_person.CreatePersonCommandHandler;
import org.ouracademy.ovejafiel.person.command.create_person.CreatePersonResult;
import org.ouracademy.ovejafiel.person.domain.MemberState;
import org.ouracademy.ovejafiel.user.command.link_user_to_person.LinkUserToPersonCommand;
import org.ouracademy.ovejafiel.user.command.link_user_to_person.LinkUserToPersonCommandHandler;
import org.ouracademy.ovejafiel.user.domain.UserId;

class AddVisitorCommandHandlerImplTests {

  FindEventOrganizationQueryHandler eventOrganization =
      mock(FindEventOrganizationQueryHandler.class);
  FindChurchOfOrganizationQueryHandler churches = mock(FindChurchOfOrganizationQueryHandler.class);
  CreatePersonCommandHandler createPerson = mock(CreatePersonCommandHandler.class);
  MarkPersonPresentCommandHandler markPerson = mock(MarkPersonPresentCommandHandler.class);
  LinkUserToPersonCommandHandler linkUser = mock(LinkUserToPersonCommandHandler.class);
  AddVisitorCommandHandlerImpl handler =
      new AddVisitorCommandHandlerImpl(
          eventOrganization, churches, createPerson, markPerson, linkUser);

  EventId eventId = EventId.generate();
  PartyId organizationId = PartyId.generate();
  PartyId personId = PartyId.generate();
  PersonInfo info = new PersonInfo("Rosa", "Díaz", "999", null, null, null, null);

  void eventAndPerson() {
    when(eventOrganization.handle(new FindEventOrganizationQuery(eventId)))
        .thenReturn(Optional.of(new EventOrganizationResult(organizationId)));
    when(createPerson.handle(org.mockito.ArgumentMatchers.any(CreatePersonCommand.class)))
        .thenReturn(new CreatePersonResult(personId));
  }

  @Test
  void registersTheVisitorInTheNodeOfTheEventAndMarksThemPresent() {
    eventAndPerson();
    var usher = UserId.generate();

    var result =
        handler.handle(
            new AddVisitorCommand("usher", eventId, info, CheckInSource.MANUAL, usher, null));

    assertThat(result.personId()).isEqualTo(personId);
    var created = ArgumentCaptor.forClass(CreatePersonCommand.class);
    verify(createPerson).handle(created.capture());
    assertThat(created.getValue().subject()).isEqualTo("usher");
    assertThat(created.getValue().organizationId()).isEqualTo(organizationId);
    assertThat(created.getValue().positionIds()).isEmpty();
    assertThat(created.getValue().memberState()).isEqualTo(MemberState.VISITOR);
    verify(markPerson)
        .handle(new MarkPersonPresentCommand(eventId, personId, CheckInSource.MANUAL, usher));
    verify(linkUser, never()).handle(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void aVisitorWhoScannedWithAnAccountBecomesThePersonOfThatAccount() {
    eventAndPerson();
    var churchId = PartyId.generate();
    when(churches.handle(new FindChurchOfOrganizationQuery(organizationId)))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(churchId)));
    var account = UserId.generate();

    handler.handle(
        new AddVisitorCommand(null, eventId, info, CheckInSource.QR_EVENT, null, account));

    verify(linkUser).handle(new LinkUserToPersonCommand(account, personId, churchId));
    verify(markPerson)
        .handle(new MarkPersonPresentCommand(eventId, personId, CheckInSource.QR_EVENT, null));
  }

  @Test
  void anUnknownEventCreatesNobody() {
    when(eventOrganization.handle(new FindEventOrganizationQuery(eventId)))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                handler.handle(
                    new AddVisitorCommand("u", eventId, info, CheckInSource.MANUAL, null, null)))
        .isInstanceOf(NoSuchElementException.class);
    verify(createPerson, never()).handle(org.mockito.ArgumentMatchers.any());
  }
}
