package org.ouracademy.ovejafiel.attendance.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.attendance.command.add_visitor.AddVisitorCommand;
import org.ouracademy.ovejafiel.attendance.command.add_visitor.AddVisitorCommandHandler;
import org.ouracademy.ovejafiel.attendance.command.mark_self_attendance.MarkSelfAttendanceCommand;
import org.ouracademy.ovejafiel.attendance.command.mark_self_attendance.MarkSelfAttendanceCommandHandler;
import org.ouracademy.ovejafiel.attendance.command.mark_user_present.MarkUserPresentCommand;
import org.ouracademy.ovejafiel.attendance.command.mark_user_present.MarkUserPresentCommandHandler;
import org.ouracademy.ovejafiel.attendance.command.set_attendance_guests.SetAttendanceGuestsCommand;
import org.ouracademy.ovejafiel.attendance.command.set_attendance_guests.SetAttendanceGuestsCommandHandler;
import org.ouracademy.ovejafiel.attendance.controller.dto.AttendanceQrResponse;
import org.ouracademy.ovejafiel.attendance.controller.dto.AttendanceWelcomeResponse;
import org.ouracademy.ovejafiel.attendance.controller.dto.CheckInResponse;
import org.ouracademy.ovejafiel.attendance.controller.dto.GuestsRequest;
import org.ouracademy.ovejafiel.attendance.controller.dto.GuestsResponse;
import org.ouracademy.ovejafiel.attendance.controller.dto.RegistrationCheckInRequest;
import org.ouracademy.ovejafiel.attendance.controller.dto.SelfAttendanceRequest;
import org.ouracademy.ovejafiel.attendance.controller.dto.SelfAttendanceResponse;
import org.ouracademy.ovejafiel.attendance.controller.dto.TicketCheckInRequest;
import org.ouracademy.ovejafiel.attendance.controller.dto.VisitorRequest;
import org.ouracademy.ovejafiel.attendance.controller.dto.VisitorResponse;
import org.ouracademy.ovejafiel.attendance.query.get_attendance_qr.GetAttendanceQrQuery;
import org.ouracademy.ovejafiel.attendance.query.get_attendance_qr.GetAttendanceQrQueryHandler;
import org.ouracademy.ovejafiel.attendance.query.get_attendance_welcome.GetAttendanceWelcomeQuery;
import org.ouracademy.ovejafiel.attendance.query.get_attendance_welcome.GetAttendanceWelcomeQueryHandler;
import org.ouracademy.ovejafiel.event.command.CheckInResult;
import org.ouracademy.ovejafiel.event.command.check_in_registration_by_ticket.CheckInRegistrationByTicketCommand;
import org.ouracademy.ovejafiel.event.command.check_in_registration_by_ticket.CheckInRegistrationByTicketCommandHandler;
import org.ouracademy.ovejafiel.event.command.set_registration_check_in.SetRegistrationCheckInCommand;
import org.ouracademy.ovejafiel.event.command.set_registration_check_in.SetRegistrationCheckInCommandHandler;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.person.command.PersonInfo;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQuery;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Marking who came, besides taking the list: the QR of the event that people scan themselves, the
 * QR of the ticket an usher scans, visitors and the count of those without a name. Marking others
 * needs {@code attendance.write} on the event's node; scanning the QR of the event needs nothing,
 * it is the usher who shows it.
 */
@RestController
@AllArgsConstructor
@Tag(name = "events")
public class AttendanceCheckInController {

  EventAuthorization eventAuthorization;
  GetAttendanceQrQueryHandler getAttendanceQrQueryHandler;
  GetAttendanceWelcomeQueryHandler getAttendanceWelcomeQueryHandler;
  MarkSelfAttendanceCommandHandler markSelfAttendanceCommandHandler;
  CheckInRegistrationByTicketCommandHandler checkInRegistrationByTicketCommandHandler;
  SetRegistrationCheckInCommandHandler setRegistrationCheckInCommandHandler;
  MarkUserPresentCommandHandler markUserPresentCommandHandler;
  AddVisitorCommandHandler addVisitorCommandHandler;
  SetAttendanceGuestsCommandHandler setAttendanceGuestsCommandHandler;
  FindUserBySubjectQueryHandler findUserBySubjectQueryHandler;

  /** The code for the QR the usher shows; it changes every few minutes. */
  @GetMapping("events/{id}/attendance/qr")
  public AttendanceQrResponse qr(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    eventAuthorization.require(jwt, id, Permission.ATTENDANCE_WRITE);
    var result = getAttendanceQrQueryHandler.handle(new GetAttendanceQrQuery(new EventId(id)));
    return new AttendanceQrResponse(result.token(), result.refreshAt());
  }

  /** What someone sees after scanning the QR; 404 when the code is not one of ours. */
  @GetMapping("public/attendance/{token}")
  public AttendanceWelcomeResponse welcome(@PathVariable String token) {
    return getAttendanceWelcomeQueryHandler
        .handle(new GetAttendanceWelcomeQuery(token))
        .map(AttendanceWelcomeResponse::from)
        .orElseThrow(() -> new NoSuchElementException("Attendance code not found"));
  }

  /** Marks the attendance of whoever scanned the QR, with or without an account. */
  @PostMapping("public/attendance/{token}")
  public SelfAttendanceResponse markMyself(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable String token,
      @RequestBody(required = false) @Valid SelfAttendanceRequest request) {
    return SelfAttendanceResponse.from(
        markSelfAttendanceCommandHandler.handle(
            new MarkSelfAttendanceCommand(
                token,
                jwt == null ? null : jwt.getSubject(),
                request == null ? null : request.name())));
  }

  /** An usher scanned the QR of a ticket. */
  @PostMapping("events/{id}/attendance/check-in")
  public CheckInResponse checkInWithTicket(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid TicketCheckInRequest request) {
    eventAuthorization.require(jwt, id, Permission.ATTENDANCE_WRITE);
    var result =
        checkInRegistrationByTicketCommandHandler.handle(
            new CheckInRegistrationByTicketCommand(new EventId(id), request.ticketCode()));
    markLinkedPerson(jwt, id, result, CheckInSource.QR_TICKET);
    return CheckInResponse.from(result);
  }

  /** Marks a sign-up as present by hand, or takes the mark back. */
  @PutMapping("events/{id}/registrations/{registrationId}/check-in")
  public CheckInResponse setCheckIn(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @PathVariable UUID registrationId,
      @RequestBody RegistrationCheckInRequest request) {
    eventAuthorization.require(jwt, id, Permission.ATTENDANCE_WRITE);
    var result =
        setRegistrationCheckInCommandHandler.handle(
            new SetRegistrationCheckInCommand(
                new EventId(id), new EventRegistrationId(registrationId), request.checkedIn()));
    if (request.checkedIn()) {
      markLinkedPerson(jwt, id, result, CheckInSource.MANUAL);
    }
    return CheckInResponse.from(result);
  }

  /** A person who comes for the first time: registered as a visitor and marked as present. */
  @PostMapping("events/{id}/attendance/visitors")
  @ResponseStatus(HttpStatus.CREATED)
  public VisitorResponse addVisitor(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid VisitorRequest request) {
    eventAuthorization.require(jwt, id, Permission.ATTENDANCE_WRITE);
    var result =
        addVisitorCommandHandler.handle(
            new AddVisitorCommand(
                jwt.getSubject(),
                new EventId(id),
                new PersonInfo(
                    request.firstName(),
                    request.lastName(),
                    request.phoneNumber(),
                    request.email(),
                    null,
                    null,
                    null),
                CheckInSource.MANUAL,
                markerOf(jwt),
                null));
    return new VisitorResponse(result.personId().value());
  }

  /** How many visitors without a name came. */
  @PutMapping("events/{id}/attendance/guests")
  public GuestsResponse setGuests(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid GuestsRequest request) {
    eventAuthorization.require(jwt, id, Permission.ATTENDANCE_WRITE);
    return new GuestsResponse(
        setAttendanceGuestsCommandHandler
            .handle(new SetAttendanceGuestsCommand(new EventId(id), request.guestCount()))
            .guestCount());
  }

  /** Whoever signed up with an account that is a person of the church is marked there as well. */
  private void markLinkedPerson(Jwt jwt, UUID eventId, CheckInResult result, CheckInSource source) {
    if (result.userId() != null) {
      markUserPresentCommandHandler.handle(
          new MarkUserPresentCommand(new EventId(eventId), result.userId(), source, markerOf(jwt)));
    }
  }

  private UserId markerOf(Jwt jwt) {
    return findUserBySubjectQueryHandler
        .handle(new FindUserBySubjectQuery(jwt.getSubject()))
        .map(user -> user.id())
        .orElse(null);
  }
}
