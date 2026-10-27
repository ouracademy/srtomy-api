package org.ouracademy.ovejafiel.attendance.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.attendance.command.register_attendance.RegisterAttendanceCommand;
import org.ouracademy.ovejafiel.attendance.command.register_attendance.RegisterAttendanceCommandHandler;
import org.ouracademy.ovejafiel.attendance.command.register_attendance.RegisterAttendanceResult;
import org.ouracademy.ovejafiel.attendance.query.get_attendance.GetAttendanceQuery;
import org.ouracademy.ovejafiel.attendance.query.get_attendance.GetAttendanceQueryHandler;
import org.ouracademy.ovejafiel.attendance.query.get_attendance.GetAttendanceResult;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.query.find_event_organization.EventOrganizationResult;
import org.ouracademy.ovejafiel.event.query.find_event_organization.FindEventOrganizationQueryHandler;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.SecurityConfig;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(AttendanceController.class)
@ContextConfiguration(classes = OvejafielApplication.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
class AttendanceControllerTests {

  @Autowired MockMvcTester mvc;
  @Autowired JwtEncoder jwtEncoder;

  @MockitoBean GetAttendanceQueryHandler getAttendanceQueryHandler;
  @MockitoBean RegisterAttendanceCommandHandler registerAttendanceCommandHandler;
  @MockitoBean FindEventOrganizationQueryHandler findEventOrganizationQueryHandler;
  @MockitoBean AuthorizeQueryHandler authorizeQueryHandler;

  static final PartyId EVENT_ORGANIZATION = PartyId.generate();

  @BeforeEach
  void everyEventBelongsToTheSameNode() {
    given(findEventOrganizationQueryHandler.handle(any()))
        .willReturn(Optional.of(new EventOrganizationResult(EVENT_ORGANIZATION)));
  }

  private String bearerHeader() {
    return "Bearer " + TestSecurityConfig.validToken(jwtEncoder);
  }

  @Test
  void getAttendanceOfAnEventWithoutAttendanceReturnsNoContent() {
    var eventId = EventId.generate();
    given(getAttendanceQueryHandler.handle(new GetAttendanceQuery(eventId)))
        .willReturn(Optional.empty());

    assertThat(
            mvc.get()
                .uri("/v1/events/" + eventId.value() + "/attendance")
                .header("Authorization", bearerHeader()))
        .hasStatus(204);
  }

  @Test
  void getAttendanceReturnsTheAttendeeIds() {
    var eventId = EventId.generate();
    var personId = PartyId.generate();
    given(getAttendanceQueryHandler.handle(new GetAttendanceQuery(eventId)))
        .willReturn(Optional.of(new GetAttendanceResult(eventId, List.of(personId))));

    var response =
        assertThat(
            mvc.get()
                .uri("/v1/events/" + eventId.value() + "/attendance")
                .header("Authorization", bearerHeader()));

    response.hasStatusOk();
    response.bodyJson().extractingPath("$.personIds[0]").isEqualTo(personId.value().toString());
  }

  @Test
  void registerAttendanceMapsTheRequestToACommandAndReturns201() {
    var eventId = EventId.generate();
    var personId = UUID.randomUUID();
    given(registerAttendanceCommandHandler.handle(any()))
        .willReturn(new RegisterAttendanceResult(eventId, List.of(new PartyId(personId))));

    assertThat(
            mvc.put()
                .uri("/v1/events/" + eventId.value() + "/attendance")
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"personIds\": [\"%s\"]}".formatted(personId)))
        .hasStatus(201);

    verify(registerAttendanceCommandHandler)
        .handle(new RegisterAttendanceCommand(eventId, List.of(new PartyId(personId))));
  }

  @Test
  void registerAttendanceWithoutPersonIdsIsRejected() {
    assertThat(
            mvc.put()
                .uri("/v1/events/" + UUID.randomUUID() + "/attendance")
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"personIds\": null}"))
        .hasStatus(400);

    verifyNoInteractions(registerAttendanceCommandHandler);
  }

  @Test
  void registerAttendanceForAMissingEventReturnsA404Problem() {
    given(registerAttendanceCommandHandler.handle(any())).willThrow(new NoSuchElementException());

    assertThat(
            mvc.put()
                .uri("/v1/events/" + UUID.randomUUID() + "/attendance")
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"personIds\": []}"))
        .hasStatus(404)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
  }

  @Test
  void takingAttendanceNeedsAttendanceWriteOnTheEventsNode() {
    given(
            authorizeQueryHandler.handle(
                new AuthorizeQuery("test-user", EVENT_ORGANIZATION, Permission.ATTENDANCE_WRITE)))
        .willThrow(new AccessDeniedException("no"));

    assertThat(
            mvc.put()
                .uri("/v1/events/" + UUID.randomUUID() + "/attendance")
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"personIds\": []}"))
        .hasStatus(403);
    verifyNoInteractions(registerAttendanceCommandHandler);
  }
}
