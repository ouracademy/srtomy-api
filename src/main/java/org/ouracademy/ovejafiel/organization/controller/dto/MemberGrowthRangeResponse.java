package org.ouracademy.ovejafiel.organization.controller.dto;

import java.time.LocalDate;
import java.util.List;
import org.ouracademy.ovejafiel.accountability.query.get_member_growth_for_range.MemberGrowthRangeResult;

/** How many people the node has in each state and how that moved over an explicit date range. */
public record MemberGrowthRangeResponse(Counts current, List<BucketResponse> buckets) {

  public record Counts(long visitors, long attendees, long members, long total) {}

  public record BucketResponse(
      LocalDate periodStart,
      LocalDate periodEnd,
      String label,
      long joined,
      long becameAttendee,
      long becameMember,
      long left,
      long peopleAtEnd,
      long membersAtEnd) {}

  public static MemberGrowthRangeResponse from(MemberGrowthRangeResult result) {
    var current = result.current();
    return new MemberGrowthRangeResponse(
        new Counts(current.visitors(), current.attendees(), current.members(), current.total()),
        result.buckets().stream()
            .map(
                bucket ->
                    new BucketResponse(
                        bucket.periodStart(),
                        bucket.periodEnd(),
                        bucket.label(),
                        bucket.joined(),
                        bucket.becameAttendee(),
                        bucket.becameMember(),
                        bucket.left(),
                        bucket.peopleAtEnd(),
                        bucket.membersAtEnd()))
            .toList());
  }
}
