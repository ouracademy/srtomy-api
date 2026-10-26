package org.ouracademy.ovejafiel.event.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;
import static org.ouracademy.ovejafiel.person.PersonFixtures.person;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.ovejafiel.event.command.create_event.CreateEventCommand;
import org.ouracademy.ovejafiel.event.command.create_event.CreateEventCommandHandler;
import org.ouracademy.ovejafiel.event.command.create_event.CreateEventResult;
import org.ouracademy.ovejafiel.event.command.delete_event.DeleteEventCommand;
import org.ouracademy.ovejafiel.event.command.delete_event.DeleteEventCommandHandler;
import org.ouracademy.ovejafiel.event.command.delete_event.DeleteEventResult;
import org.ouracademy.ovejafiel.event.command.edit_event.EditEventCommandHandler;
import org.ouracademy.ovejafiel.event.domain.AccessLevel;
import org.ouracademy.ovejafiel.event.domain.EventChangeOption;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.query.FindEventResult;
import org.ouracademy.ovejafiel.event.query.authorize_event.AuthorizeEventQuery;
import org.ouracademy.ovejafiel.event.query.authorize_event.AuthorizeEventQueryHandler;
import org.ouracademy.ovejafiel.event.query.authorize_event.AuthorizeEventResult;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQuery;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQueryHandler;
import org.ouracademy.ovejafiel.event.query.search_events.SearchEventsQuery;
import org.ouracademy.ovejafiel.event.query.search_events.SearchEventsQueryHandler;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.SecurityConfig;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(EventController.class)
@ContextConfiguration(classes = OvejafielApplication.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
class EventControllerTests {

  @Autowired MockMvcTester mvc;
  @Autowired JwtEncoder jwtEncoder;

  @MockitoBean SearchEventsQueryHandler searchEventsQueryHandler;
  @MockitoBean FindEventByIdQueryHandler findEventByIdQueryHandler;
  @MockitoBean CreateEventCommandHandler createEventCommandHandler;
  @MockitoBean DeleteEventCommandHandler deleteEventCommandHandler;
  @MockitoBean EditEventCommandHandler editEventCommandHandler;
  @MockitoBean AuthorizeEventQueryHandler authorizeEventQueryHandler;
  @MockitoBean AuthorizeQueryHandler authorizeQueryHandler;

  @BeforeEach
  void everyoneCanDoEverythingWithAnEventUnlessTold() {
    given(authorizeEventQueryHandler.handle(any()))
        .willReturn(new AuthorizeEventResult(AccessLevel.MANAGER));
  }

  private String bearerHeader() {
    return "Bearer " + TestSecurityConfig.validToken(jwtEncoder);
  }

  private static FindEventResult eventResult(EventId id) {
    var church = organization(PartyId.generate(), "Iglesia Central", Organization.Type.CHURCH);
    var speaker = person(PartyId.generate(), "Jose", "Mena");
    return FindEventResult.from(singleEvent(id, "Viernes con Jesus", church, List.of(speaker)));
  }

  @Test
  void findAllForAnUnknownOrganizationAndDateRangeReturnsAnEmptyList() {
    var organizationId = UUID.randomUUID();
    given(searchEventsQueryHandler.handle(any())).willReturn(List.of());

    assertThat(
            mvc.get()
                .uri(
                    "/v1/events?organizationId="
                        + organizationId
                        + "&start=2026-01-01T00:00:00Z&end=2026-12-31T00:00:00Z")
                .header("Authorization", bearerHeader()))
        .hasStatusOk()
        .bodyText()
        .isEqualTo("[]");
  }

  @Test
  void findAllSearchesTheOrganizationsEventsInUtcAndReturnsTheEventFields() {
    var organizationId = UUID.randomUUID();
    var eventId = EventId.generate();
    given(
            searchEventsQueryHandler.handle(
                new SearchEventsQuery(
                    new PartyId(organizationId),
                    ZonedDateTime.parse("2026-01-01T00:00:00Z[UTC]"),
                    ZonedDateTime.parse("2026-12-31T00:00:00Z[UTC]"))))
        .willReturn(List.of(eventResult(eventId)));

    var response =
        assertThat(
            mvc.get()
                .uri(
                    "/v1/events?organizationId="
                        + organizationId
                        + "&start=2026-01-01T00:00:00&end=2026-12-31T00:00:00")
                .header("Authorization", bearerHeader()));

    response.hasStatusOk();
    response.bodyJson().extractingPath("$[0].id").isEqualTo(eventId.value().toString());
    response.bodyJson().extractingPath("$[0].title").isEqualTo("Viernes con Jesus");
    response.bodyJson().extractingPath("$[0].isACourse").isEqualTo(false);
    response.bodyJson().extractingPath("$[0].organization.name").isEqualTo("Iglesia Central");
    response.bodyJson().extractingPath("$[0].organization.deletable").isEqualTo(false);
    response.bodyJson().extractingPath("$[0].speakers[0].fullName").isEqualTo("Jose Mena");
  }

  @Test
  void findByIdReturnsTheEvent() {
    var eventId = EventId.generate();
    given(findEventByIdQueryHandler.handle(new FindEventByIdQuery(eventId)))
        .willReturn(Optional.of(eventResult(eventId)));

    var response =
        assertThat(
            mvc.get().uri("/v1/events/" + eventId.value()).header("Authorization", bearerHeader()));

    response.hasStatusOk();
    response.bodyJson().extractingPath("$.id").isEqualTo(eventId.value().toString());
    response.bodyJson().extractingPath("$.address").isEqualTo("Av. Siempre Viva 123");
  }

  @Test
  void findByIdOfAMissingEventReturns404() {
    given(findEventByIdQueryHandler.handle(any())).willReturn(Optional.empty());

    assertThat(
            mvc.get()
                .uri("/v1/events/" + UUID.randomUUID())
                .header("Authorization", bearerHeader()))
        .hasStatus(404);
  }

  @Test
  void createMapsTheRequestToACommandAndReturnsTheNewId() {
    var organizationId = UUID.randomUUID();
    var speakerId = UUID.randomUUID();
    var eventId = EventId.generate();
    given(createEventCommandHandler.handle(any())).willReturn(new CreateEventResult(eventId));

    assertThat(
            mvc.post()
                .uri("/v1/events")
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "Servicio", "startTime": "2026-01-05T10:00:00-05:00",
                     "endTime": "2026-01-05T12:00:00-05:00", "organizationId": "%s",
                     "speakerIds": ["%s"], "address": "Av. 1", "description": "d",
                     "isACourse": true, "daysOfWeek": [1, 3], "endRecur": "2026-01-14"}
                    """
                        .formatted(organizationId, speakerId)))
        .hasStatus(201)
        .bodyText()
        .isEqualTo("\"" + eventId.value() + "\"");

    var command = ArgumentCaptor.forClass(CreateEventCommand.class);
    verify(createEventCommandHandler).handle(command.capture());
    assertThat(command.getValue())
        .isEqualTo(
            new CreateEventCommand(
                "Servicio",
                OffsetDateTime.parse("2026-01-05T10:00:00-05:00"),
                OffsetDateTime.parse("2026-01-05T12:00:00-05:00"),
                "Av. 1",
                "d",
                true,
                new PartyId(organizationId),
                List.of(new PartyId(speakerId)),
                List.of(1, 3),
                java.time.LocalDate.of(2026, 1, 14),
                true));
  }

  @Test
  void createARecurrentEventWithoutDaysOfWeekIsRejected() {
    assertThat(
            mvc.post()
                .uri("/v1/events")
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "Servicio", "startTime": "2026-01-05T10:00:00-05:00",
                     "endTime": "2026-01-05T12:00:00-05:00", "organizationId": "%s",
                     "endRecur": "2026-01-14"}
                    """
                        .formatted(UUID.randomUUID())))
        .hasStatus(400);

    verifyNoInteractions(createEventCommandHandler);
  }

  @Test
  void deleteDefaultsToThisEventOnly() {
    var eventId = EventId.generate();
    given(deleteEventCommandHandler.handle(any())).willReturn(new DeleteEventResult(eventId));

    assertThat(
            mvc.method(HttpMethod.DELETE)
                .uri("/v1/events/" + eventId.value())
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .hasStatusOk();

    verify(deleteEventCommandHandler)
        .handle(new DeleteEventCommand(eventId, EventChangeOption.THIS_EVENT));
  }

  @Test
  void deletingAMissingEventReturnsA404Problem() {
    // Exercises GlobalExceptionHandler end-to-end: DeleteEventCommandHandler throws
    // NoSuchElementException for an id that doesn't exist.
    given(deleteEventCommandHandler.handle(any())).willThrow(new NoSuchElementException());

    assertThat(
            mvc.method(HttpMethod.DELETE)
                .uri("/v1/events/" + UUID.randomUUID())
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"option\": \"ALL_EVENTS\"}"))
        .hasStatus(404)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
  }

  @Test
  void editIsNotImplementedYet() {
    given(editEventCommandHandler.handle(any())).willThrow(new NotImplementedException("TODO:"));

    assertThat(
            mvc.put()
                .uri("/v1/events/" + UUID.randomUUID())
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "Servicio", "startTime": "2026-01-05T10:00:00-05:00",
                     "endTime": "2026-01-05T12:00:00-05:00", "organizationId": "%s",
                     "option": "THIS_EVENT"}
                    """
                        .formatted(UUID.randomUUID())))
        .hasStatus(500)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
  }

  @Test
  void anEventThatDoesNotExistIsNotFoundBeforeAnyPermissionCheck() {
    given(authorizeEventQueryHandler.handle(any())).willThrow(new NoSuchElementException("no"));

    assertThat(
            mvc.get()
                .uri("/v1/events/" + UUID.randomUUID())
                .header("Authorization", bearerHeader()))
        .hasStatus(404);
    verifyNoInteractions(findEventByIdQueryHandler);
  }

  @Test
  void deletingAnEventNeedsToManageIt() {
    var id = UUID.randomUUID();
    given(
            authorizeEventQueryHandler.handle(
                new AuthorizeEventQuery("test-user", new EventId(id), AccessLevel.MANAGER)))
        .willThrow(new AccessDeniedException("no"));

    assertThat(
            mvc.method(HttpMethod.DELETE)
                .uri("/v1/events/" + id)
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .hasStatus(403);
    verifyNoInteractions(deleteEventCommandHandler);
  }

  @Test
  void readingAnEventNeedsAtLeastViewing() {
    var id = UUID.randomUUID();
    given(
            authorizeEventQueryHandler.handle(
                new AuthorizeEventQuery("test-user", new EventId(id), AccessLevel.VIEWER)))
        .willThrow(new AccessDeniedException("no"));

    assertThat(mvc.get().uri("/v1/events/" + id).header("Authorization", bearerHeader()))
        .hasStatus(403);
    verifyNoInteractions(findEventByIdQueryHandler);
  }

  @Test
  void listingEventsNeedsEventsReadOnTheNode() {
    var organizationId = UUID.randomUUID();
    given(
            authorizeQueryHandler.handle(
                new AuthorizeQuery(
                    "test-user", new PartyId(organizationId), Permission.EVENTS_READ)))
        .willThrow(new AccessDeniedException("no"));

    assertThat(
            mvc.get()
                .uri(
                    "/v1/events?organizationId="
                        + organizationId
                        + "&start=2026-01-01T00:00:00&end=2026-02-01T00:00:00")
                .header("Authorization", bearerHeader()))
        .hasStatus(403);
    verifyNoInteractions(searchEventsQueryHandler);
  }
}
