package org.ouracademy.ovejafiel.attendance.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.attendance.command.register_attendance.RegisterAttendanceCommand;
import org.ouracademy.ovejafiel.attendance.command.register_attendance.RegisterAttendanceCommandHandler;
import org.ouracademy.ovejafiel.attendance.controller.dto.AttendanceRequest;
import org.ouracademy.ovejafiel.attendance.controller.dto.EventAttendanceResponse;
import org.ouracademy.ovejafiel.attendance.query.get_attendance.GetAttendanceQuery;
import org.ouracademy.ovejafiel.attendance.query.get_attendance.GetAttendanceQueryHandler;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.iam.domain.Permission;
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
  EventAuthorization eventAuthorization;

  @GetMapping("{id}/attendance")
  public ResponseEntity<EventAttendanceResponse> getAttendance(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    eventAuthorization.require(jwt, id, Permission.EVENTS_READ);
    return getAttendanceQueryHandler
        .handle(new GetAttendanceQuery(new EventId(id)))
        .map(
            result ->
                EventAttendanceResponse.builder()
                    .personIds(result.personIds().stream().map(PartyId::value).toList())
                    .guestCount(result.guestCount())
                    .marks(
                        result.marks().stream()
                            .map(
                                mark ->
                                    new EventAttendanceResponse.Mark(
                                        mark.personId().value(), mark.source(), mark.markedAt()))
                            .toList())
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
    eventAuthorization.require(jwt, id, Permission.ATTENDANCE_WRITE);
    registerAttendanceCommandHandler.handle(
        new RegisterAttendanceCommand(
            new EventId(id), attendanceRequest.personIds().stream().map(PartyId::new).toList()));
  }
}
