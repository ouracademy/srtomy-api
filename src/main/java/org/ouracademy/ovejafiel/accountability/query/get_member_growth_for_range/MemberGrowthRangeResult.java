package org.ouracademy.ovejafiel.accountability.query.get_member_growth_for_range;

import java.time.LocalDate;
import java.util.List;

/**
 * The people of a node over an explicit range: {@code current} counts them by state today and
 * {@code buckets} (oldest first, every period of the range even if nothing happened) tells how that
 * moved.
 */
public record MemberGrowthRangeResult(StateCounts current, List<Bucket> buckets) {

  public record StateCounts(long visitors, long attendees, long members) {
    public long total() {
      return visitors + attendees + members;
    }
  }

  /**
   * One period of the range: people who {@code joined} the node, who {@code becameAttendee} or
   * {@code becameMember} after being something else there, and who {@code left}; plus how many
   * people and members the node had when the period ended. {@code label} is a human string for the
   * period ("2026-10-05" for a day/week bucket, "2026-10" for a month bucket).
   */
  public record Bucket(
      LocalDate periodStart,
      LocalDate periodEnd,
      String label,
      long joined,
      long becameAttendee,
      long becameMember,
      long left,
      long peopleAtEnd,
      long membersAtEnd) {}
}
