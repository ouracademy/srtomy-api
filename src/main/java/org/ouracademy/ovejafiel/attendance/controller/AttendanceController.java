package org.ouracademy.ovejafiel.attendance.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.attendance.command.register_attendance.RegisterAttendanceCommand;
import org.ouracademy.ovejafiel.attendance.command.register_attendance.RegisterAttendanceCommandHandler;
import org.ouracademy.ovejafiel.attendance.controller.dto.AttendanceRequest;
import org.ouracademy.ovejafiel.attendance.controller.dto.EventAttendanceResponse;
import org.ouracademy.ovejafiel.attendance.query.get_attendance.GetAttendanceQuery;
import org.ouracademy.ovejafiel.attendance.query.get_attendance.GetAttendanceQueryHandler;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.query.find_event_organization.FindEventOrganizationQuery;
import org.ouracademy.ovejafiel.event.query.find_event_organization.FindEventOrganizationQueryHandler;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Attendance of an event, under the same {@code events/{id}} path as the event endpoints. */
@RestController
@RequestMapping("events")
@AllArgsConstructor
@Tag(name = "events")
public class AttendanceController {

  GetAttendanceQueryHandler getAttendanceQueryHandler;
  RegisterAttendanceCommandHandler registerAttendanceCommandHandler;
  FindEventOrganizationQueryHandler findEventOrganizationQueryHandler;
  AuthorizeQueryHandler authorizeQueryHandler;

  @GetMapping("{id}/attendance")
  public ResponseEntity<EventAttendanceResponse> getAttendance(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    authorizeOnEvent(jwt, id, Permission.EVENTS_READ);
    return getAttendanceQueryHandler
        .handle(new GetAttendanceQuery(new EventId(id)))
        .map(
            result ->
                EventAttendanceResponse.builder()
                    .personIds(result.personIds().stream().map(PartyId::value).toList())
                    .build())
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.noContent().build());
  }

  @PutMapping("{id}/attendance")
  @ResponseStatus(code = HttpStatus.CREATED)
  public void registerAttendance(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid AttendanceRequest attendanceRequest) {
    authorizeOnEvent(jwt, id, Permission.ATTENDANCE_WRITE);
    registerAttendanceCommandHandler.handle(
        new RegisterAttendanceCommand(
            new EventId(id), attendanceRequest.getPersonIds().stream().map(PartyId::new).toList()));
  }

  /** 404 when the event does not exist, 403 when the user lacks the permission on its node. */
  private void authorizeOnEvent(Jwt jwt, UUID eventId, Permission permission) {
    var organizationId =
        findEventOrganizationQueryHandler
            .handle(new FindEventOrganizationQuery(new EventId(eventId)))
            .orElseThrow(() -> new NoSuchElementException("Event not found: " + eventId))
            .organizationId();
    authorizeQueryHandler.handle(new AuthorizeQuery(jwt.getSubject(), organizationId, permission));
  }
}
