package org.ouracademy.ovejafiel.attendance.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.attendance.query.get_attendance_stats.GetAttendanceStatsQuery;
import org.ouracademy.ovejafiel.attendance.query.get_attendance_stats.GetAttendanceStatsQueryHandler;
import org.ouracademy.ovejafiel.attendance.query.get_attendance_stats.GetAttendanceStatsResult;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeResult;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.BucketUnit;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.SecurityConfig;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(AttendanceStatsController.class)
@ContextConfiguration(classes = OvejafielApplication.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
class AttendanceStatsControllerTests {

  @Autowired MockMvcTester mvc;
  @Autowired JwtEncoder jwtEncoder;

  @MockitoBean GetAttendanceStatsQueryHandler getAttendanceStatsQueryHandler;
  @MockitoBean FindOrganizationSubtreeQueryHandler findOrganizationSubtreeQueryHandler;
  @MockitoBean AuthorizeQueryHandler authorizeQueryHandler;

  String bearer() {
    return "Bearer " + TestSecurityConfig.validToken(jwtEncoder);
  }

  @Test
  void returnsTheBucketsOfTheNodeAndItsDescendants() {
    var id = PartyId.generate();
    var below = PartyId.generate();
    given(findOrganizationSubtreeQueryHandler.handle(any()))
        .willReturn(new FindOrganizationSubtreeResult(List.of(id, below)));
    given(getAttendanceStatsQueryHandler.handle(any()))
        .willReturn(
            new GetAttendanceStatsResult(
                List.of(
                    new GetAttendanceStatsResult.Bucket(
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 1),
                        "2026-10-01",
                        2,
                        15,
                        10,
                        7.5))));

    var response =
        assertThat(
                mvc.get()
                    .uri(
                        "/v1/organizations/"
                            + id.value()
                            + "/attendance-stats?startDate=2026-10-01&endDate=2026-10-01")
                    .header("Authorization", bearer()))
            .hasStatusOk()
            .bodyJson();
    response.extractingPath("$.buckets[0].label").isEqualTo("2026-10-01");
    response.extractingPath("$.buckets[0].eventsCount").isEqualTo(2);
    response.extractingPath("$.buckets[0].attendanceMarks").isEqualTo(15);
    response.extractingPath("$.buckets[0].uniqueAttendees").isEqualTo(10);
    response.extractingPath("$.buckets[0].averagePerEvent").isEqualTo(7.5);

    verify(getAttendanceStatsQueryHandler)
        .handle(
            new GetAttendanceStatsQuery(
                List.of(id, below),
                LocalDate.of(2026, 10, 1).atStartOfDay(ZoneOffset.UTC).toInstant(),
                LocalDate.of(2026, 10, 1)
                    .plusDays(1)
                    .atStartOfDay(ZoneOffset.UTC)
                    .minusNanos(1)
                    .toInstant(),
                BucketUnit.DAY));
    verify(authorizeQueryHandler)
        .handle(new AuthorizeQuery("test-user", id, Permission.EVENTS_READ));
  }

  @Test
  void needsEventsRead() {
    doThrow(new AccessDeniedException("no")).when(authorizeQueryHandler).handle(any());

    assertThat(
            mvc.get()
                .uri(
                    "/v1/organizations/"
                        + PartyId.generate().value()
                        + "/attendance-stats?startDate=2026-10-01&endDate=2026-10-02")
                .header("Authorization", bearer()))
        .hasStatus(403);
    verifyNoInteractions(getAttendanceStatsQueryHandler);
  }

  @Test
  void withStartAfterEndIsBadRequest() {
    var id = PartyId.generate();

    assertThat(
            mvc.get()
                .uri(
                    "/v1/organizations/"
                        + id.value()
                        + "/attendance-stats?startDate=2026-10-05&endDate=2026-10-01")
                .header("Authorization", bearer()))
        .hasStatus(400);
    verifyNoInteractions(getAttendanceStatsQueryHandler);
  }
}
