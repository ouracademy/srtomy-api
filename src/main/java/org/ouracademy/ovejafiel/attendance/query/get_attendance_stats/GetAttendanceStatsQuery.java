package org.ouracademy.ovejafiel.attendance.query.get_attendance_stats;

import java.time.Instant;
import java.util.List;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.BucketUnit;

/**
 * Attendance over {@code organizationIds} (already resolved, e.g. a node and everything below it)
 * between {@code start} and {@code end}, sliced into {@code bucket}-sized periods.
 */
public record GetAttendanceStatsQuery(
    List<PartyId> organizationIds, Instant start, Instant end, BucketUnit bucket) {}
