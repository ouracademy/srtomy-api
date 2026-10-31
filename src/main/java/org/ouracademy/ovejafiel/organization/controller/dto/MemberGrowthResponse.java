package org.ouracademy.ovejafiel.organization.controller.dto;

import java.util.List;
import org.ouracademy.ovejafiel.accountability.query.get_member_growth.MemberGrowthResult;

/** How many people the node has in each state and how that moved month by month. */
public record MemberGrowthResponse(Counts current, List<MonthResponse> months) {

  public record Counts(long visitors, long attendees, long members, long total) {}

  public record MonthResponse(
      String month,
      long joined,
      long becameAttendee,
      long becameMember,
      long left,
      long peopleAtEnd,
      long membersAtEnd) {}

  public static MemberGrowthResponse from(MemberGrowthResult result) {
    var current = result.current();
    return new MemberGrowthResponse(
        new Counts(current.visitors(), current.attendees(), current.members(), current.total()),
        result.months().stream()
            .map(
                month ->
                    new MonthResponse(
                        month.month(),
                        month.joined(),
                        month.becameAttendee(),
                        month.becameMember(),
                        month.left(),
                        month.peopleAtEnd(),
                        month.membersAtEnd()))
            .toList());
  }
}
