package org.ouracademy.ovejafiel.attendance.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.attendance.controller.dto.AttendanceStatsResponse;
import org.ouracademy.ovejafiel.attendance.query.get_attendance_stats.GetAttendanceStatsQuery;
import org.ouracademy.ovejafiel.attendance.query.get_attendance_stats.GetAttendanceStatsQueryHandler;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.BucketUnit;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * How many events happened and how attendance moved for a node (and everything below it) over a
 * date range. Under {@code organizations/{id}}, like the growth endpoints, rather than under {@code
 * events} (which is about a single event).
 */
@RestController
@RequestMapping("organizations")
@AllArgsConstructor
@Tag(name = "organizations")
public class AttendanceStatsController {

  GetAttendanceStatsQueryHandler getAttendanceStatsQueryHandler;
  FindOrganizationSubtreeQueryHandler findOrganizationSubtreeQueryHandler;
  AuthorizeQueryHandler authorizeQueryHandler;

  /** Same bucket choice as {@code OrganizationController.growthRange}: day, week or month. */
  @GetMapping("{id}/attendance-stats")
  public AttendanceStatsResponse attendanceStats(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    authorizeQueryHandler.handle(
        new AuthorizeQuery(jwt.getSubject(), new PartyId(id), Permission.EVENTS_READ));
    if (startDate.isAfter(endDate)) {
      throw new DomainValidationException("La fecha de inicio no puede ser posterior a la de fin");
    }
    var subtree =
        findOrganizationSubtreeQueryHandler.handle(
            new FindOrganizationSubtreeQuery(new PartyId(id)));
    var start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    var end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).minusNanos(1).toInstant();
    return AttendanceStatsResponse.from(
        getAttendanceStatsQueryHandler.handle(
            new GetAttendanceStatsQuery(subtree.ids(), start, end, bucketFor(startDate, endDate))));
  }

  private static BucketUnit bucketFor(LocalDate startDate, LocalDate endDate) {
    var days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
    if (days <= 31) {
      return BucketUnit.DAY;
    }
    if (days <= 120) {
      return BucketUnit.WEEK;
    }
    return BucketUnit.MONTH;
  }
}
