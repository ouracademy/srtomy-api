package org.ouracademy.ovejafiel.attendance.command.mark_self_attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.eventBetween;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.ovejafiel.attendance.command.add_visitor.AddVisitorCommand;
import org.ouracademy.ovejafiel.attendance.command.add_visitor.AddVisitorCommandHandler;
import org.ouracademy.ovejafiel.attendance.command.add_visitor.AddVisitorResult;
import org.ouracademy.ovejafiel.attendance.command.mark_self_attendance.MarkSelfAttendanceResult.Kind;
import org.ouracademy.ovejafiel.attendance.command.mark_user_present.MarkUserPresentCommand;
import org.ouracademy.ovejafiel.attendance.command.mark_user_present.MarkUserPresentCommandHandler;
import org.ouracademy.ovejafiel.attendance.command.mark_user_present.MarkUserPresentResult;
import org.ouracademy.ovejafiel.attendance.query.AttendanceQrToken;
import org.ouracademy.ovejafiel.attendance.repository.EventAttendanceRepository;
import org.ouracademy.ovejafiel.event.command.CheckInResult;
import org.ouracademy.ovejafiel.event.command.check_in_user_registration.CheckInUserRegistrationCommand;
import org.ouracademy.ovejafiel.event.command.check_in_user_registration.CheckInUserRegistrationCommandHandler;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;
import org.ouracademy.ovejafiel.event.query.FindEventResult;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQuery;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQueryHandler;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.query.UserResult;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQuery;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;

class MarkSelfAttendanceCommandHandlerImplTests {

  AttendanceQrToken token = new AttendanceQrToken("a-long-enough-test-secret");
  FindEventByIdQueryHandler events = mock(FindEventByIdQueryHandler.class);
  FindUserBySubjectQueryHandler users = mock(FindUserBySubjectQueryHandler.class);
  MarkUserPresentCommandHandler markUser = mock(MarkUserPresentCommandHandler.class);
  CheckInUserRegistrationCommandHandler checkIn = mock(CheckInUserRegistrationCommandHandler.class);
  AddVisitorCommandHandler addVisitor = mock(AddVisitorCommandHandler.class);
  EventAttendanceRepository attendances = mock(EventAttendanceRepository.class);
  MarkSelfAttendanceCommandHandlerImpl handler =
      new MarkSelfAttendanceCommandHandlerImpl(
          token, events, users, markUser, checkIn, addVisitor, attendances);

  EventId eventId = EventId.generate();
  UserId userId = UserId.generate();
  UserResult user = new UserResult(userId, "me", "me@x.org", true);
  String code;

  void eventAt(ZonedDateTime start, ZonedDateTime end) {
    Event event =
        eventBetween(
            eventId,
            "Culto",
            organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
            start,
            end);
    when(events.handle(new FindEventByIdQuery(eventId)))
        .thenReturn(Optional.of(FindEventResult.from(event)));
  }

  @BeforeEach
  void setUp() {
    eventAt(ZonedDateTime.now().minusMinutes(30), ZonedDateTime.now().plusHours(1));
    code = token.issue(eventId, Instant.now());
    when(users.handle(new FindUserBySubjectQuery("me"))).thenReturn(Optional.of(user));
    when(markUser.handle(any())).thenReturn(Optional.empty());
    when(checkIn.handle(any())).thenReturn(Optional.empty());
  }

  @Test
  void anAccountLinkedToAPersonMarksThatPerson() {
    var personId = PartyId.generate();
    when(markUser.handle(new MarkUserPresentCommand(eventId, userId, CheckInSource.QR_EVENT, null)))
        .thenReturn(Optional.of(new MarkUserPresentResult(personId, false)));

    var result = handler.handle(new MarkSelfAttendanceCommand(code, "me", null));

    assertThat(result).isEqualTo(new MarkSelfAttendanceResult(Kind.PERSON, false));
    verify(addVisitor, never()).handle(any());
    verify(attendances, never()).addGuests(any(), org.mockito.ArgumentMatchers.anyInt());
  }

  @Test
  void scanningAgainSaysTheyAreAlreadyMarked() {
    when(markUser.handle(any()))
        .thenReturn(Optional.of(new MarkUserPresentResult(PartyId.generate(), true)));

    assertThat(handler.handle(new MarkSelfAttendanceCommand(code, "me", null)).alreadyMarked())
        .isTrue();
  }

  @Test
  void theTicketOfTheAccountIsCheckedInAsWell() {
    when(checkIn.handle(new CheckInUserRegistrationCommand(eventId, userId, "me@x.org")))
        .thenReturn(
            Optional.of(
                new CheckInResult(
                    EventRegistrationId.generate(), userId, "Me Me", "General", false, null)));

    var result = handler.handle(new MarkSelfAttendanceCommand(code, "me", null));

    assertThat(result).isEqualTo(new MarkSelfAttendanceResult(Kind.REGISTRATION, false));
    verify(addVisitor, never()).handle(any());
  }

  @Test
  void anUnverifiedEmailDoesNotFindTheTicket() {
    when(users.handle(new FindUserBySubjectQuery("me")))
        .thenReturn(Optional.of(new UserResult(userId, "me", "me@x.org", false)));

    handler.handle(new MarkSelfAttendanceCommand(code, "me", null));

    verify(checkIn).handle(new CheckInUserRegistrationCommand(eventId, userId, null));
  }

  @Test
  void anAccountWithoutAPersonAndWithANameBecomesAVisitorLinkedToIt() {
    when(addVisitor.handle(any())).thenReturn(new AddVisitorResult(PartyId.generate()));

    var result = handler.handle(new MarkSelfAttendanceCommand(code, "me", "  Rosa  Díaz de Soto "));

    assertThat(result.kind()).isEqualTo(Kind.VISITOR);
    var command = ArgumentCaptor.forClass(AddVisitorCommand.class);
    verify(addVisitor).handle(command.capture());
    assertThat(command.getValue().info().firstName()).isEqualTo("Rosa");
    assertThat(command.getValue().info().lastName()).isEqualTo("Díaz de Soto");
    assertThat(command.getValue().linkTo()).isEqualTo(userId);
    assertThat(command.getValue().source()).isEqualTo(CheckInSource.QR_EVENT);
    assertThat(command.getValue().markedBy()).isNull();
  }

  @Test
  void someoneWithoutAnAccountAndWithANameBecomesAnUnlinkedVisitor() {
    when(addVisitor.handle(any())).thenReturn(new AddVisitorResult(PartyId.generate()));

    var result = handler.handle(new MarkSelfAttendanceCommand(code, null, "Rosa"));

    assertThat(result.kind()).isEqualTo(Kind.VISITOR);
    var command = ArgumentCaptor.forClass(AddVisitorCommand.class);
    verify(addVisitor).handle(command.capture());
    assertThat(command.getValue().info().firstName()).isEqualTo("Rosa");
    assertThat(command.getValue().info().lastName()).isEmpty();
    assertThat(command.getValue().linkTo()).isNull();
    verify(users, never()).handle(any());
  }

  @Test
  void someoneWithoutAnAccountOrANameIsJustCounted() {
    var result = handler.handle(new MarkSelfAttendanceCommand(code, null, "   "));

    assertThat(result).isEqualTo(new MarkSelfAttendanceResult(Kind.ANONYMOUS, false));
    verify(attendances).createIfMissing(eventId.value());
    verify(attendances).addGuests(eventId, 1);
    verify(addVisitor, never()).handle(any());
  }

  @Test
  void anAlteredCodeIsNotFound() {
    assertThatThrownBy(() -> handler.handle(new MarkSelfAttendanceCommand(code + "x", null, null)))
        .isInstanceOf(NoSuchElementException.class);
    assertThatThrownBy(() -> handler.handle(new MarkSelfAttendanceCommand("nonsense", null, null)))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void anExpiredCodeAsksForTheCurrentOne() {
    var old = token.issue(eventId, Instant.now().minusSeconds(3600));

    assertThatThrownBy(() -> handler.handle(new MarkSelfAttendanceCommand(old, null, null)))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("venció");
    verify(attendances, never()).addGuests(any(), org.mockito.ArgumentMatchers.anyInt());
  }

  @Test
  void anEventNotOnNowDoesNotTakeAttendance() {
    eventAt(ZonedDateTime.now().plusDays(2), ZonedDateTime.now().plusDays(2).plusHours(2));

    assertThatThrownBy(() -> handler.handle(new MarkSelfAttendanceCommand(code, null, "Rosa")))
        .isInstanceOf(DomainValidationException.class);

    eventAt(ZonedDateTime.now().minusDays(2), ZonedDateTime.now().minusDays(2).plusHours(2));
    assertThatThrownBy(() -> handler.handle(new MarkSelfAttendanceCommand(code, null, "Rosa")))
        .isInstanceOf(DomainValidationException.class);
    verify(addVisitor, never()).handle(any());
  }

  @Test
  void aCodeOfAnEventThatNoLongerExistsIsNotFound() {
    when(events.handle(new FindEventByIdQuery(eventId))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(new MarkSelfAttendanceCommand(code, null, null)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
