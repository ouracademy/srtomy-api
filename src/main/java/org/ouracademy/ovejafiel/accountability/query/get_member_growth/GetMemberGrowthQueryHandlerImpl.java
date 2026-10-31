package org.ouracademy.ovejafiel.accountability.query.get_member_growth;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.query.get_member_growth.MemberGrowthResult.Month;
import org.ouracademy.ovejafiel.accountability.query.get_member_growth.MemberGrowthResult.StateCounts;
import org.ouracademy.ovejafiel.accountability.repository.MemberStateEventRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Replays the history of states: a person has a state per node, and in a node with sub-nodes they
 * count with the highest one. Replaying is what lets the same event feed a group's numbers and its
 * church's, and tell how many people and members there were when each month ended.
 */
@Service
@RequiredArgsConstructor
class GetMemberGrowthQueryHandlerImpl implements GetMemberGrowthQueryHandler {

  private static final int NONE = 0;
  private static final int VISITOR = 1;
  private static final int ATTENDEE = 2;
  private static final int MEMBER = 3;

  private final MemberStateEventRepository repository;

  @Override
  @Transactional(readOnly = true)
  public MemberGrowthResult handle(GetMemberGrowthQuery query) {
    var first = YearMonth.from(LocalDate.now()).minusMonths(query.months() - 1L);
    var months = new ArrayList<Tally>();
    for (var i = 0; i < query.months(); i++) {
      months.add(new Tally(first.plusMonths(i)));
    }

    var stateByNode = new HashMap<UUID, Map<UUID, Integer>>();
    var overall = new HashMap<UUID, Integer>();
    var people = new Counter();
    var next = 0;
    var events =
        repository.findByOrganizations(
            query.organizationIds().stream().map(PartyId::value).toList());
    for (var event : events) {
      // Close the months that ended before this event with the totals they ended with.
      var month = YearMonth.from(event.getOccurredAt());
      while (next < months.size() && months.get(next).month.isBefore(month)) {
        months.get(next++).endWith(people);
      }

      var nodes = stateByNode.computeIfAbsent(event.getPersonId(), id -> new HashMap<>());
      if (event.getToState() == null) {
        nodes.remove(event.getOrganizationId());
      } else {
        nodes.put(event.getOrganizationId(), rankOf(event.getToState()));
      }
      var before = overall.getOrDefault(event.getPersonId(), NONE);
      var after = nodes.values().stream().mapToInt(Integer::intValue).max().orElse(NONE);
      overall.put(event.getPersonId(), after);
      people.move(before, after);

      if (!month.isBefore(first)) {
        months.get((int) ChronoUnit.MONTHS.between(first, month)).record(before, after);
      }
    }
    while (next < months.size()) {
      months.get(next++).endWith(people);
    }
    return new MemberGrowthResult(
        new StateCounts(people.visitors, people.attendees, people.members),
        months.stream().map(Tally::toMonth).toList());
  }

  private static int rankOf(String state) {
    return switch (state) {
      case "MEMBER" -> MEMBER;
      case "MEMBERSHIP_IN_PROGRESS" -> ATTENDEE;
      default -> VISITOR;
    };
  }

  /** How many people there are in each state right now. */
  private static final class Counter {
    long visitors;
    long attendees;
    long members;

    void move(int from, int to) {
      add(from, -1);
      add(to, 1);
    }

    private void add(int rank, int by) {
      switch (rank) {
        case VISITOR -> visitors += by;
        case ATTENDEE -> attendees += by;
        case MEMBER -> members += by;
        default -> {
          // not in the node
        }
      }
    }

    long total() {
      return visitors + attendees + members;
    }
  }

  /** What happened in one month and how many people there were when it ended. */
  private static final class Tally {
    final YearMonth month;
    long joined;
    long becameAttendee;
    long becameMember;
    long left;
    long peopleAtEnd;
    long membersAtEnd;

    Tally(YearMonth month) {
      this.month = month;
    }

    void record(int before, int after) {
      if (before == after) {
        return;
      }
      if (before == NONE) {
        joined++;
      } else if (after == NONE) {
        left++;
      } else if (after > before && after == ATTENDEE) {
        becameAttendee++;
      } else if (after > before && after == MEMBER) {
        becameMember++;
      }
    }

    void endWith(Counter people) {
      peopleAtEnd = people.total();
      membersAtEnd = people.members;
    }

    Month toMonth() {
      return new Month(
          month.toString(), joined, becameAttendee, becameMember, left, peopleAtEnd, membersAtEnd);
    }
  }
}
