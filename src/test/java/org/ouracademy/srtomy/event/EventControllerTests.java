package org.ouracademy.srtomy.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.SecurityConfig;
import org.ouracademy.srtomy.TestSecurityConfig;
import org.ouracademy.srtomy.organization.OrganizationRepository;
import org.ouracademy.srtomy.person.PartyId;
import org.ouracademy.srtomy.person.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(EventController.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
class EventControllerTests {

  @Autowired MockMvcTester mvc;
  @Autowired JwtEncoder jwtEncoder;

  @MockitoBean EventRepository eventRepository;
  @MockitoBean SingleEventRepository singleEventRepository;
  @MockitoBean PersonRepository personRepository;
  @MockitoBean OrganizationRepository organizationRepository;
  @MockitoBean EventAttendanceRepository eventAttendanceRepository;

  private String bearerHeader() {
    return "Bearer " + TestSecurityConfig.validToken(jwtEncoder);
  }

  @Test
  void findAllForAnUnknownOrganizationAndDateRangeReturnsAnEmptyList() {
    var organizationId = UUID.randomUUID();
    given(
            eventRepository.searchEventsOfOrgIdAndBetweenDates(
                eq(new PartyId(organizationId)), any(), any()))
        .willReturn(List.of());

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
  void deletingAMissingEventReturnsA404Problem() {
    // Exercises GlobalExceptionHandler end-to-end: singleEventRepository.findById(id)
    // .orElseThrow() in EventController.delete throws NoSuchElementException for an id
    // that doesn't exist.
    var missingId = UUID.randomUUID();
    given(singleEventRepository.findById(new EventId(missingId))).willReturn(Optional.empty());

    assertThat(
            mvc.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/v1/events/" + missingId)
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .hasStatus(404)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
  }
}
