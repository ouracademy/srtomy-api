package org.ouracademy.ovejafiel.accountability.query.get_member_growth;

import java.util.List;

/**
 * The people of a node: {@code current} counts them by state today and {@code months} (oldest
 * first, every month of the range even if nothing happened) tells how that moved.
 */
public record MemberGrowthResult(StateCounts current, List<Month> months) {

  public record StateCounts(long visitors, long attendees, long members) {
    public long total() {
      return visitors + attendees + members;
    }
  }

  /**
   * One month ("2026-10"): people who {@code joined} the node, who {@code becameAttendee} or {@code
   * becameMember} after being something else there, and who {@code left}; plus how many people and
   * members the node had when the month ended.
   */
  public record Month(
      String month,
      long joined,
      long becameAttendee,
      long becameMember,
      long left,
      long peopleAtEnd,
      long membersAtEnd) {}
}
