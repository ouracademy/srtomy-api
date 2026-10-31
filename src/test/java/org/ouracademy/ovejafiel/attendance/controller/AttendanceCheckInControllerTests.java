package org.ouracademy.ovejafiel.attendance.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.ovejafiel.attendance.command.add_visitor.AddVisitorCommand;
import org.ouracademy.ovejafiel.attendance.command.add_visitor.AddVisitorCommandHandler;
import org.ouracademy.ovejafiel.attendance.command.add_visitor.AddVisitorResult;
import org.ouracademy.ovejafiel.attendance.command.mark_self_attendance.MarkSelfAttendanceCommand;
import org.ouracademy.ovejafiel.attendance.command.mark_self_attendance.MarkSelfAttendanceCommandHandler;
import org.ouracademy.ovejafiel.attendance.command.mark_self_attendance.MarkSelfAttendanceResult;
import org.ouracademy.ovejafiel.attendance.command.mark_user_present.MarkUserPresentCommand;
import org.ouracademy.ovejafiel.attendance.command.mark_user_present.MarkUserPresentCommandHandler;
import org.ouracademy.ovejafiel.attendance.command.mark_user_present.MarkUserPresentResult;
import org.ouracademy.ovejafiel.attendance.command.set_attendance_guests.SetAttendanceGuestsCommand;
import org.ouracademy.ovejafiel.attendance.command.set_attendance_guests.SetAttendanceGuestsCommandHandler;
import org.ouracademy.ovejafiel.attendance.command.set_attendance_guests.SetAttendanceGuestsResult;
import org.ouracademy.ovejafiel.attendance.query.get_attendance_qr.GetAttendanceQrQueryHandler;
import org.ouracademy.ovejafiel.attendance.query.get_attendance_qr.GetAttendanceQrResult;
import org.ouracademy.ovejafiel.attendance.query.get_attendance_welcome.GetAttendanceWelcomeQuery;
import org.ouracademy.ovejafiel.attendance.query.get_attendance_welcome.GetAttendanceWelcomeQueryHandler;
import org.ouracademy.ovejafiel.attendance.query.get_attendance_welcome.GetAttendanceWelcomeResult;
import org.ouracademy.ovejafiel.event.command.CheckInResult;
import org.ouracademy.ovejafiel.event.command.check_in_registration_by_ticket.CheckInRegistrationByTicketCommand;
import org.ouracademy.ovejafiel.event.command.check_in_registration_by_ticket.CheckInRegistrationByTicketCommandHandler;
import org.ouracademy.ovejafiel.event.command.set_registration_check_in.SetRegistrationCheckInCommand;
import org.ouracademy.ovejafiel.event.command.set_registration_check_in.SetRegistrationCheckInCommandHandler;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;
import org.ouracademy.ovejafiel.event.query.find_event_organization.EventOrganizationResult;
import org.ouracademy.ovejafiel.event.query.find_event_organization.FindEventOrganizationQueryHandler;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.SecurityConfig;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.query.UserResult;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(AttendanceCheckInController.class)
@ContextConfiguration(classes = OvejafielApplication.class)
@Import({TestSecurityConfig.class, SecurityConfig.class, EventAuthorization.class})
class AttendanceCheckInControllerTests {

  @Autowired MockMvcTester mvc;
  @Autowired JwtEncoder jwtEncoder;

  @MockitoBean FindEventOrganizationQueryHandler findEventOrganizationQueryHandler;
  @MockitoBean AuthorizeQueryHandler authorizeQueryHandler;
  @MockitoBean GetAttendanceQrQueryHandler getAttendanceQrQueryHandler;
  @MockitoBean GetAttendanceWelcomeQueryHandler getAttendanceWelcomeQueryHandler;
  @MockitoBean MarkSelfAttendanceCommandHandler markSelfAttendanceCommandHandler;
  @MockitoBean CheckInRegistrationByTicketCommandHandler checkInRegistrationByTicketCommandHandler;
  @MockitoBean SetRegistrationCheckInCommandHandler setRegistrationCheckInCommandHandler;
  @MockitoBean MarkUserPresentCommandHandler markUserPresentCommandHandler;
  @MockitoBean AddVisitorCommandHandler addVisitorCommandHandler;
  @MockitoBean SetAttendanceGuestsCommandHandler setAttendanceGuestsCommandHandler;
  @MockitoBean FindUserBySubjectQueryHandler findUserBySubjectQueryHandler;

  static final PartyId NODE = PartyId.generate();
  UUID id = UUID.randomUUID();
  UserId usher = UserId.generate();

  @BeforeEach
  void everyEventBelongsToTheSameNode() {
    given(findEventOrganizationQueryHandler.handle(any()))
        .willReturn(Optional.of(new EventOrganizationResult(NODE)));
    given(findUserBySubjectQueryHandler.handle(any()))
        .willReturn(Optional.of(new UserResult(usher, "test-user", "u@x.org", true)));
  }

  String bearer() {
    return "Bearer " + TestSecurityConfig.validToken(jwtEncoder);
  }

  CheckInResult checkedIn(UserId account) {
    return new CheckInResult(
        EventRegistrationId.generate(),
        account,
        "Ana Paz",
        "General",
        false,
        LocalDateTime.parse("2026-03-01T10:00:00"));
  }

  @Test
  void theUsherGetsTheCodeForTheQr() {
    given(getAttendanceQrQueryHandler.handle(any()))
        .willReturn(new GetAttendanceQrResult("the-code", Instant.parse("2026-03-01T10:05:00Z")));

    var body =
        assertThat(
                mvc.get()
                    .uri("/v1/events/" + id + "/attendance/qr")
                    .header("Authorization", bearer()))
            .hasStatusOk()
            .bodyJson();
    body.extractingPath("$.token").isEqualTo("the-code");
    verify(authorizeQueryHandler)
        .handle(new AuthorizeQuery("test-user", NODE, Permission.ATTENDANCE_WRITE));
  }

  @Test
  void everythingThatMarksOthersNeedsToTakeAttendance() {
    given(authorizeQueryHandler.handle(any())).willThrow(new AccessDeniedException("no"));
    var event = "/v1/events/" + id;

    for (var request :
        java.util.List.of(
            mvc.get().uri(event + "/attendance/qr"),
            mvc.post()
                .uri(event + "/attendance/check-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticketCode\":\"x\"}"),
            mvc.put()
                .uri(event + "/registrations/" + UUID.randomUUID() + "/check-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"checkedIn\":true}"),
            mvc.post()
                .uri(event + "/attendance/visitors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Rosa\"}"),
            mvc.put()
                .uri(event + "/attendance/guests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"guestCount\":2}"))) {
      assertThat(request.header("Authorization", bearer())).hasStatus(403);
    }
    verifyNoInteractions(
        getAttendanceQrQueryHandler,
        checkInRegistrationByTicketCommandHandler,
        setRegistrationCheckInCommandHandler,
        addVisitorCommandHandler,
        setAttendanceGuestsCommandHandler);
  }

  @Test
  void scanningATicketChecksInAndMarksThePersonOfTheAccount() {
    var account = UserId.generate();
    given(checkInRegistrationByTicketCommandHandler.handle(any())).willReturn(checkedIn(account));
    given(markUserPresentCommandHandler.handle(any()))
        .willReturn(Optional.of(new MarkUserPresentResult(PartyId.generate(), false)));

    var body =
        assertThat(
                mvc.post()
                    .uri("/v1/events/" + id + "/attendance/check-in")
                    .header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ticketCode\":\"abc\"}"))
            .hasStatusOk()
            .bodyJson();
    body.extractingPath("$.name").isEqualTo("Ana Paz");
    body.extractingPath("$.alreadyCheckedIn").isEqualTo(false);
    verify(checkInRegistrationByTicketCommandHandler)
        .handle(new CheckInRegistrationByTicketCommand(new EventId(id), "abc"));
    verify(markUserPresentCommandHandler)
        .handle(
            new MarkUserPresentCommand(new EventId(id), account, CheckInSource.QR_TICKET, usher));
  }

  @Test
  void aTicketWithoutAnAccountOnlyChecksInTheSignUp() {
    given(checkInRegistrationByTicketCommandHandler.handle(any())).willReturn(checkedIn(null));

    assertThat(
            mvc.post()
                .uri("/v1/events/" + id + "/attendance/check-in")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticketCode\":\"abc\"}"))
        .hasStatusOk();
    verifyNoInteractions(markUserPresentCommandHandler);
  }

  private CheckInResult inheritedCheckIn(UserId account) {
    return new CheckInResult(
        EventRegistrationId.generate(), account, "Ana Paz", "General", false, null, true);
  }

  @Test
  void theContainersTicketMarksThePersonInTheSubEventWithoutTouchingTheSignUp() {
    var account = UserId.generate();
    given(checkInRegistrationByTicketCommandHandler.handle(any()))
        .willReturn(inheritedCheckIn(account));
    given(markUserPresentCommandHandler.handle(any()))
        .willReturn(Optional.of(new MarkUserPresentResult(PartyId.generate(), false)));

    var body =
        assertThat(
                mvc.post()
                    .uri("/v1/events/" + id + "/attendance/check-in")
                    .header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ticketCode\":\"abc\"}"))
            .hasStatusOk()
            .bodyJson();
    body.extractingPath("$.name").isEqualTo("Ana Paz");
    body.extractingPath("$.alreadyCheckedIn").isEqualTo(false);
    verify(markUserPresentCommandHandler)
        .handle(
            new MarkUserPresentCommand(new EventId(id), account, CheckInSource.QR_TICKET, usher));
  }

  @Test
  void scanningTheContainersTicketAgainInTheSubEventSaysTheyAlreadyCame() {
    var account = UserId.generate();
    given(checkInRegistrationByTicketCommandHandler.handle(any()))
        .willReturn(inheritedCheckIn(account));
    given(markUserPresentCommandHandler.handle(any()))
        .willReturn(Optional.of(new MarkUserPresentResult(PartyId.generate(), true)));

    assertThat(
            mvc.post()
                .uri("/v1/events/" + id + "/attendance/check-in")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticketCode\":\"abc\"}"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.alreadyCheckedIn")
        .isEqualTo(true);
  }

  @Test
  void anInheritedTicketWithoutALinkedPersonIsRejectedInsteadOfSayingTheyCame() {
    given(checkInRegistrationByTicketCommandHandler.handle(any()))
        .willReturn(inheritedCheckIn(null));
    var withoutAccount =
        assertThat(
            mvc.post()
                .uri("/v1/events/" + id + "/attendance/check-in")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticketCode\":\"abc\"}"));
    withoutAccount.hasStatus(400);

    var account = UserId.generate();
    given(checkInRegistrationByTicketCommandHandler.handle(any()))
        .willReturn(inheritedCheckIn(account));
    given(markUserPresentCommandHandler.handle(any())).willReturn(Optional.empty());
    assertThat(
            mvc.post()
                .uri("/v1/events/" + id + "/attendance/check-in")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticketCode\":\"abc\"}"))
        .hasStatus(400);
  }

  @Test
  void aTicketWithoutCodeOrOfNothingIsRejected() {
    assertThat(
            mvc.post()
                .uri("/v1/events/" + id + "/attendance/check-in")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticketCode\":\"\"}"))
        .hasStatus(400);
    org.mockito.BDDMockito.willThrow(new NoSuchElementException())
        .given(checkInRegistrationByTicketCommandHandler)
        .handle(any());
    assertThat(
            mvc.post()
                .uri("/v1/events/" + id + "/attendance/check-in")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticketCode\":\"nope\"}"))
        .hasStatus(404);
    org.mockito.BDDMockito.willThrow(new DomainValidationException("en espera"))
        .given(checkInRegistrationByTicketCommandHandler)
        .handle(any());
    assertThat(
            mvc.post()
                .uri("/v1/events/" + id + "/attendance/check-in")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticketCode\":\"waiting\"}"))
        .hasStatus(400);
  }

  @Test
  void markingASignUpByHandAlsoMarksItsPersonButTakingTheMarkBackDoesNot() {
    var account = UserId.generate();
    var registrationId = UUID.randomUUID();
    given(setRegistrationCheckInCommandHandler.handle(any())).willReturn(checkedIn(account));
    given(markUserPresentCommandHandler.handle(any())).willReturn(Optional.empty());
    var uri = "/v1/events/" + id + "/registrations/" + registrationId + "/check-in";

    assertThat(
            mvc.put()
                .uri(uri)
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"checkedIn\":true}"))
        .hasStatusOk();
    verify(setRegistrationCheckInCommandHandler)
        .handle(
            new SetRegistrationCheckInCommand(
                new EventId(id), new EventRegistrationId(registrationId), true));
    verify(markUserPresentCommandHandler)
        .handle(new MarkUserPresentCommand(new EventId(id), account, CheckInSource.MANUAL, usher));

    org.mockito.Mockito.clearInvocations(markUserPresentCommandHandler);
    assertThat(
            mvc.put()
                .uri(uri)
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"checkedIn\":false}"))
        .hasStatusOk();
    verifyNoInteractions(markUserPresentCommandHandler);
  }

  @Test
  void addsAVisitorWithOnlyTheFirstNameRequired() {
    var personId = PartyId.generate();
    given(addVisitorCommandHandler.handle(any())).willReturn(new AddVisitorResult(personId));

    var body =
        assertThat(
                mvc.post()
                    .uri("/v1/events/" + id + "/attendance/visitors")
                    .header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"firstName\":\"Rosa\",\"phoneNumber\":\"999\"}"))
            .hasStatus(201)
            .bodyJson();
    body.extractingPath("$.personId").isEqualTo(personId.value().toString());
    var command = ArgumentCaptor.forClass(AddVisitorCommand.class);
    verify(addVisitorCommandHandler).handle(command.capture());
    assertThat(command.getValue().info().firstName()).isEqualTo("Rosa");
    assertThat(command.getValue().info().lastName()).isNull();
    assertThat(command.getValue().info().phoneNumber()).isEqualTo("999");
    assertThat(command.getValue().source()).isEqualTo(CheckInSource.MANUAL);
    assertThat(command.getValue().markedBy()).isEqualTo(usher);
    assertThat(command.getValue().linkTo()).isNull();
  }

  @Test
  void aVisitorNeedsAFirstNameAndAValidEmail() {
    for (var body :
        java.util.List.of(
            "{}", "{\"firstName\":\" \"}", "{\"firstName\":\"R\",\"email\":\"no\"}")) {
      assertThat(
              mvc.post()
                  .uri("/v1/events/" + id + "/attendance/visitors")
                  .header("Authorization", bearer())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .hasStatus(400);
    }
    verifyNoInteractions(addVisitorCommandHandler);
  }

  @Test
  void setsTheGuestsWithoutAName() {
    given(setAttendanceGuestsCommandHandler.handle(any()))
        .willReturn(new SetAttendanceGuestsResult(5));

    var body =
        assertThat(
                mvc.put()
                    .uri("/v1/events/" + id + "/attendance/guests")
                    .header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"guestCount\":5}"))
            .hasStatusOk()
            .bodyJson();
    body.extractingPath("$.guestCount").isEqualTo(5);
    verify(setAttendanceGuestsCommandHandler)
        .handle(new SetAttendanceGuestsCommand(new EventId(id), 5));
    assertThat(
            mvc.put()
                .uri("/v1/events/" + id + "/attendance/guests")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"guestCount\":-1}"))
        .hasStatus(400);
  }

  @Test
  void theWelcomeOfAScannedCodeNeedsNoSession() {
    given(getAttendanceWelcomeQueryHandler.handle(new GetAttendanceWelcomeQuery("code")))
        .willReturn(
            Optional.of(
                new GetAttendanceWelcomeResult(
                    "Culto",
                    ZonedDateTime.parse("2026-03-01T10:00:00Z"),
                    "Iglesia",
                    GetAttendanceWelcomeResult.Status.OPEN)));

    var body = assertThat(mvc.get().uri("/v1/public/attendance/code")).hasStatusOk().bodyJson();
    body.extractingPath("$.title").isEqualTo("Culto");
    body.extractingPath("$.status").isEqualTo("OPEN");
  }

  @Test
  void aCodeWeDidNotMakeIsNotFound() {
    given(getAttendanceWelcomeQueryHandler.handle(any())).willReturn(Optional.empty());

    assertThat(mvc.get().uri("/v1/public/attendance/forged")).hasStatus(404);
  }

  @Test
  void anyoneScansWithoutASessionAndWithoutABody() {
    given(markSelfAttendanceCommandHandler.handle(any()))
        .willReturn(new MarkSelfAttendanceResult(MarkSelfAttendanceResult.Kind.ANONYMOUS, false));

    var body = assertThat(mvc.post().uri("/v1/public/attendance/code")).hasStatusOk().bodyJson();
    body.extractingPath("$.kind").isEqualTo("ANONYMOUS");
    verify(markSelfAttendanceCommandHandler)
        .handle(new MarkSelfAttendanceCommand("code", null, null));
  }

  @Test
  void aTokenKeepsTheAccountAndTheNameIsPassedAlong() {
    given(markSelfAttendanceCommandHandler.handle(any()))
        .willReturn(new MarkSelfAttendanceResult(MarkSelfAttendanceResult.Kind.VISITOR, false));

    assertThat(
            mvc.post()
                .uri("/v1/public/attendance/code")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Rosa Díaz\"}"))
        .hasStatusOk();
    verify(markSelfAttendanceCommandHandler)
        .handle(new MarkSelfAttendanceCommand("code", "test-user", "Rosa Díaz"));
  }

  @Test
  void anExpiredOrOutOfWindowScanIsABadRequestAndAForgedOneNotFound() {
    given(markSelfAttendanceCommandHandler.handle(any()))
        .willThrow(new DomainValidationException("venció"))
        .willThrow(new NoSuchElementException());

    assertThat(mvc.post().uri("/v1/public/attendance/old")).hasStatus(400);
    assertThat(mvc.post().uri("/v1/public/attendance/forged")).hasStatus(404);
  }
}
